package cn.org.alan.exam.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lionsoul.ip2region.xdb.Searcher;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * IP 工具：从请求头链解析真实客户端 IP，并结合 classpath 内 {@code ipdata/ip2region.xdb} 查询行政区划文本；
 * {@link Searcher} 按线程存放，用完须 {@link #closeSearcher()} 以防泄漏。
 *
 * @author Alan
 */
public class IPUtils {

    private static final Logger log = LogManager.getLogger(IPUtils.class);

    private static final String DB_PATH;
    
    static {
        String tempPath = null;
        try {
            // 从 classpath 读取 IP 数据库文件
            InputStream inputStream = IPUtils.class.getClassLoader().getResourceAsStream("ipdata/ip2region.xdb");
            if (inputStream != null) {
                // 复制到临时文件
                Path tempFile = Files.createTempFile("ip2region", ".xdb");
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempPath = tempFile.toString();
                inputStream.close();
                log.info("IP 数据库加载成功: {}", tempPath);
            } else {
                log.warn("IP 数据库文件未找到，IP 归属地查询功能将不可用");
            }
        } catch (Exception e) {
            log.error("初始化 IP 数据库失败: {}", e.getMessage());
        }
        DB_PATH = tempPath;
    }

    private static final ThreadLocal<Searcher> searcherThreadLocal = ThreadLocal.withInitial(() -> {
        if (DB_PATH == null) {
            return null;
        }
        try {
            return Searcher.newWithFileOnly(DB_PATH);
        } catch (Exception e) {
            log.error("初始化 IP 归属地查询失败: {}", e.getMessage());
            return null;
        }
    });

    /**
     * 解析请求 IP 并查询 ip2region 归属地字符串（未加载库时返回 {@code null}）。
     *
     * @param request HTTP 请求
     * @return 区域文本，如 {@code 国家|区域|省份|城市|ISP}；失败返回 {@code null}
     */
    public static String getIPRegion(HttpServletRequest request) {
        String ip = getIPAddress(request);
        Searcher searcher = searcherThreadLocal.get();
        if (searcher == null) {
            log.error("IP 归属地查询失败，返回空");
            return null;
        }
        try {
            long startTime = System.nanoTime();
            String region = searcher.search(ip);
            long cost = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime);
            log.info("IP: {}, Region: {}, IO Count: {}, Took: {} μs", ip, region, searcher.getIOCount(), cost);
            return region;
        } catch (Exception e) {
            log.error("IP: {} 获取 IP 归属地错误，错误原因: {}", ip, e.getMessage());
            return null;
        } finally {
            closeSearcher();
        }
    }

    /**
     * 依次读取常见代理头后回落 {@link HttpServletRequest#getRemoteAddr()}。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 字符串
     */
    private static String getIPAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    /**
     * 关闭当前线程持有的 Searcher 并从 ThreadLocal 移除，避免泄漏。
     */
    public static void closeSearcher() {
        try {
            Searcher searcher = searcherThreadLocal.get();
            if (Objects.nonNull(searcher)) {
                searcher.close();
                searcherThreadLocal.remove();
            }
        } catch (Exception e) {
            log.error("关闭异常", e);
        }
    }
}