package cn.org.alan.exam.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 从请求中解析真实客户端 IP，并解析为国家/省/市级登录地点。
 * <p>
 * 每次登录/登出均实时查询，不做结果缓存。本机访问时优先使用前端上报的
 * {@value #CLIENT_PUBLIC_IP_HEADER}（走浏览器网络，可反映 VPN 切换）。
 */
@Slf4j
public final class IPUtils {

    public static final String CLIENT_PUBLIC_IP_HEADER = "X-Client-Public-Ip";

    private static final String XDB_CLASSPATH = "ip2region.xdb";
    private static final int ONLINE_TIMEOUT_MS = 3000;

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private static final Set<String> USELESS_REGION_KEYWORDS = new LinkedHashSet<>();

    static {
        USELESS_REGION_KEYWORDS.add("reserved");
        USELESS_REGION_KEYWORDS.add("内网ip");
        USELESS_REGION_KEYWORDS.add("局域网");
        USELESS_REGION_KEYWORDS.add("unknown");
        USELESS_REGION_KEYWORDS.add("0");
    }

    private static volatile Searcher searcher;

    private IPUtils() {
    }

    /**
     * 获取登录地点，格式示例：中国 广东省 深圳市。
     */
    public static String getIPRegion(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String clientIp = getClientIp(request);
        if (StringUtils.isBlank(clientIp)) {
            return "未知";
        }
        String browserPublicIp = getBrowserPublicIp(request);
        String geoIp = selectGeoIp(clientIp, browserPublicIp);
        return resolveLoginPlace(geoIp, clientIp);
    }

    /**
     * 从代理头或 {@code remoteAddr} 中解析真实客户端 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex);
                }
                ip = ip.trim();
                if (StringUtils.isNotBlank(ip)) {
                    return normalizeIp(ip);
                }
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private static String getBrowserPublicIp(HttpServletRequest request) {
        String ip = request.getHeader(CLIENT_PUBLIC_IP_HEADER);
        if (!isValidPublicIpv4(ip)) {
            return null;
        }
        return normalizeIp(ip);
    }

    private static String selectGeoIp(String clientIp, String browserPublicIp) {
        if (StringUtils.isNotBlank(browserPublicIp)) {
            return browserPublicIp;
        }
        if (!isInternalIp(clientIp)) {
            return clientIp;
        }
        return resolvePublicIpEgress();
    }

    private static String resolveLoginPlace(String geoIp, String clientIp) {
        if (StringUtils.isNotBlank(geoIp)) {
            String place = geolocateIp(geoIp);
            if (isUsefulLocation(place)) {
                return place;
            }
        }

        // 与登出一致：本机/内网时走服务端出口定位（国内优先 pconline）
        if (isInternalIp(clientIp)) {
            String serverPlace = resolveServerSideLocation();
            if (isUsefulLocation(serverPlace)) {
                return serverPlace;
            }
            return "本机/内网（" + clientIp + "）";
        }

        if (StringUtils.isNotBlank(geoIp)) {
            return "未知（" + geoIp + "）";
        }
        return "未知（" + clientIp + "）";
    }

    private static String geolocateIp(String ip) {
        String offline = formatOfflineRegion(lookupRegion(ip));
        if (isUsefulLocation(offline)) {
            return offline;
        }
        return geolocateOnline(ip);
    }

    /** 服务端查询当前公网出口归属地（登出路径同款，国内可用）。 */
    private static String resolveServerSideLocation() {
        String pconlinePlace = queryPconlineEgressLocation();
        if (isUsefulLocation(pconlinePlace)) {
            return pconlinePlace;
        }

        String egressIp = resolvePublicIpEgress();
        if (StringUtils.isNotBlank(egressIp)) {
            String place = geolocateIp(egressIp);
            if (isUsefulLocation(place)) {
                return place;
            }
        }
        return null;
    }

    private static String queryPconlineEgressLocation() {
        JSONObject json = fetchPconlineJson(null);
        if (json == null) {
            return null;
        }
        String pro = json.getStr("pro");
        String city = json.getStr("city");
        if (StringUtils.isNotBlank(pro) || StringUtils.isNotBlank(city)) {
            return joinLocation("中国", pro, city);
        }
        String addr = json.getStr("addr");
        return normalizeLocationText(addr);
    }

    private static String resolvePublicIpEgress() {
        JSONObject pconline = fetchPconlineJson(null);
        if (pconline != null) {
            String ip = pconline.getStr("ip");
            if (isValidPublicIpv4(ip)) {
                return normalizeIp(ip);
            }
        }

        try {
            String body = HttpRequest.get("http://ip-api.com/json/?fields=status,query")
                    .timeout(ONLINE_TIMEOUT_MS)
                    .execute()
                    .body();
            if (JSONUtil.isTypeJSON(body)) {
                JSONObject json = JSONUtil.parseObj(body);
                if ("success".equalsIgnoreCase(json.getStr("status"))) {
                    String query = json.getStr("query");
                    if (isValidPublicIpv4(query)) {
                        return normalizeIp(query);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("ip-api 公网 IP 查询失败", ex);
        }

        try {
            String ip = HttpRequest.get("https://api.ipify.org")
                    .timeout(ONLINE_TIMEOUT_MS)
                    .execute()
                    .body();
            if (isValidPublicIpv4(ip)) {
                return normalizeIp(ip.trim());
            }
        } catch (Exception ex) {
            log.debug("ipify 公网 IP 查询失败", ex);
        }
        return null;
    }

    private static String geolocateOnline(String ip) {
        if (StringUtils.isBlank(ip)) {
            return null;
        }
        String location = queryPconline(ip);
        if (!isUsefulLocation(location)) {
            location = queryIpApi(ip);
        }
        return location;
    }

    private static JSONObject fetchPconlineJson(String ip) {
        try {
            String url = StringUtils.isBlank(ip)
                    ? "https://whois.pconline.com.cn/ipJson.jsp?json=true"
                    : "https://whois.pconline.com.cn/ipJson.jsp?json=true&ip=" + ip;
            String body = HttpRequest.get(url).timeout(ONLINE_TIMEOUT_MS).charset("GBK").execute().body();
            if (!JSONUtil.isTypeJSON(body)) {
                return null;
            }
            return JSONUtil.parseObj(body);
        } catch (Exception ex) {
            log.debug("pconline 请求失败, ip={}", ip, ex);
            return null;
        }
    }

    private static String normalizeIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return ip;
        }
        ip = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        if (ip.startsWith("::ffff:")) {
            return ip.substring("::ffff:".length());
        }
        return ip;
    }

    private static boolean isValidPublicIpv4(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        ip = normalizeIp(ip);
        if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return false;
        }
        return !isInternalIp(ip);
    }

    private static String lookupRegion(String ip) {
        Searcher regionSearcher = getSearcher();
        if (regionSearcher == null) {
            return null;
        }
        try {
            return regionSearcher.search(ip);
        } catch (Exception ex) {
            log.warn("ip2region 查询失败, ip={}", ip, ex);
            return null;
        }
    }

    private static String queryIpApi(String ip) {
        try {
            String url = "http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=status,message,country,regionName,city";
            String body = HttpRequest.get(url).timeout(ONLINE_TIMEOUT_MS).execute().body();
            if (!JSONUtil.isTypeJSON(body)) {
                return null;
            }
            JSONObject json = JSONUtil.parseObj(body);
            if (!"success".equalsIgnoreCase(json.getStr("status"))) {
                return null;
            }
            return joinLocation(json.getStr("country"), json.getStr("regionName"), json.getStr("city"));
        } catch (Exception ex) {
            log.debug("ip-api 归属地查询失败, ip={}", ip, ex);
            return null;
        }
    }

    private static String queryPconline(String ip) {
        JSONObject json = fetchPconlineJson(ip);
        if (json == null) {
            return null;
        }
        String pro = json.getStr("pro");
        String city = json.getStr("city");
        String addr = json.getStr("addr");
        if (StringUtils.isNotBlank(pro) || StringUtils.isNotBlank(city)) {
            return joinLocation("中国", pro, city);
        }
        if (StringUtils.isNotBlank(addr)) {
            return normalizeLocationText(addr);
        }
        return null;
    }

    private static String formatOfflineRegion(String rawRegion) {
        if (StringUtils.isBlank(rawRegion)) {
            return null;
        }
        String[] parts = rawRegion.split("\\|");
        if (parts.length >= 3) {
            return joinLocation(parts[0], parts[1], parts[2]);
        }
        return normalizeLocationText(rawRegion.replace('|', ' '));
    }

    private static String joinLocation(String country, String province, String city) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        addSegment(segments, country);
        addSegment(segments, province);
        addSegment(segments, city);
        if (segments.isEmpty()) {
            return null;
        }
        return String.join(" ", segments);
    }

    private static void addSegment(Set<String> segments, String value) {
        if (StringUtils.isBlank(value) || "0".equals(value.trim())) {
            return;
        }
        String normalized = value.trim();
        if (normalized.length() == 2 && normalized.equals(normalized.toUpperCase())
                && normalized.chars().allMatch(Character::isLetter)) {
            return;
        }
        if (isUselessRegionToken(normalized)) {
            return;
        }
        if (!segments.contains(normalized)) {
            segments.add(normalized);
        }
    }

    private static String normalizeLocationText(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (!isUsefulLocation(cleaned)) {
            return null;
        }
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private static boolean isUsefulLocation(String location) {
        if (StringUtils.isBlank(location)) {
            return false;
        }
        String normalized = location.trim().toLowerCase();
        for (String keyword : USELESS_REGION_KEYWORDS) {
            if (normalized.equals(keyword) || normalized.contains(keyword)) {
                return false;
            }
        }
        return location.trim().length() > 1;
    }

    private static boolean isUselessRegionToken(String token) {
        if (StringUtils.isBlank(token)) {
            return true;
        }
        String normalized = token.trim().toLowerCase();
        for (String keyword : USELESS_REGION_KEYWORDS) {
            if (normalized.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static Searcher getSearcher() {
        if (searcher != null) {
            return searcher;
        }
        synchronized (IPUtils.class) {
            if (searcher != null) {
                return searcher;
            }
            try (InputStream inputStream = IPUtils.class.getClassLoader().getResourceAsStream(XDB_CLASSPATH)) {
                if (inputStream == null) {
                    log.warn("未找到 ip2region 数据文件: {}", XDB_CLASSPATH);
                    return null;
                }
                byte[] dbBytes = toByteArray(inputStream);
                searcher = Searcher.newWithBuffer(dbBytes);
                return searcher;
            } catch (Exception ex) {
                log.warn("加载 ip2region 数据文件失败", ex);
                return null;
            }
        }
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    static boolean isInternalIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] sections = ip.split("\\.");
            if (sections.length > 1) {
                try {
                    int second = Integer.parseInt(sections[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
