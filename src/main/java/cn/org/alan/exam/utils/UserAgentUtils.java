package cn.org.alan.exam.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 HTTP {@code User-Agent} 解析可读设备描述（操作系统、浏览器、机型）。
 */
public final class UserAgentUtils {

    private static final Pattern ANDROID_MODEL = Pattern.compile("Android [^;]+;\\s*([^;)]+)\\)");
    private static final Pattern WINDOWS_NT = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern MAC_OS = Pattern.compile("Mac OS X ([\\d_]+)");
    private static final Pattern IOS_VERSION = Pattern.compile("(?:iPhone OS|CPU OS) ([\\d_]+)");
    private static final Pattern ANDROID_VERSION = Pattern.compile("Android ([\\d.]+)");

    private UserAgentUtils() {
    }

    /**
     * 生成日志展示用设备名，示例：{@code Windows 10 / Chrome}、{@code SM-G991B / Android 13 / Chrome}。
     */
    public static String extractDeviceType(String userAgent) {
        if (StringUtils.isBlank(userAgent)) {
            return "未知设备";
        }
        String ua = userAgent.trim();

        String browser = detectBrowser(ua);
        String os = detectOs(ua);
        String model = detectDeviceModel(ua);

        StringBuilder label = new StringBuilder();
        if (StringUtils.isNotBlank(model)) {
            label.append(model);
        } else if (StringUtils.isNotBlank(os)) {
            label.append(os);
        }
        if (StringUtils.isNotBlank(browser)) {
            if (label.length() > 0) {
                label.append(" / ");
            }
            label.append(browser);
        } else if (StringUtils.isNotBlank(os) && StringUtils.isNotBlank(model)) {
            if (label.length() > 0 && !label.toString().contains(os)) {
                label.append(" / ").append(os);
            }
        }

        if (label.length() == 0) {
            return ua.length() > 64 ? ua.substring(0, 64) : ua;
        }
        return label.length() > 255 ? label.substring(0, 255) : label.toString();
    }

    private static String detectBrowser(String ua) {
        if (ua.contains("Electron/")) {
            return "Electron";
        }
        if (ua.contains("Edg/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera/")) {
            return "Opera";
        }
        if (ua.contains("Firefox/")) {
            return "Firefox";
        }
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/") && !ua.contains("Chrome/") && !ua.contains("CriOS/")) {
            return "Safari";
        }
        if (ua.contains("MSIE") || ua.contains("Trident/")) {
            return "IE";
        }
        return null;
    }

    private static String detectOs(String ua) {
        if (ua.contains("iPhone")) {
            return "iOS " + normalizeVersion(findGroup(IOS_VERSION, ua));
        }
        if (ua.contains("iPad")) {
            return "iPadOS " + normalizeVersion(findGroup(IOS_VERSION, ua));
        }
        if (ua.contains("Android")) {
            return "Android " + normalizeVersion(findGroup(ANDROID_VERSION, ua));
        }
        if (ua.contains("Windows NT")) {
            return mapWindowsVersion(findGroup(WINDOWS_NT, ua));
        }
        if (ua.contains("Mac OS X")) {
            return "macOS " + normalizeVersion(findGroup(MAC_OS, ua));
        }
        if (ua.contains("CrOS")) {
            return "Chrome OS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return null;
    }

    private static String detectDeviceModel(String ua) {
        if (ua.contains("iPhone")) {
            return "iPhone";
        }
        if (ua.contains("iPad")) {
            return "iPad";
        }
        if (ua.contains("Android")) {
            Matcher matcher = ANDROID_MODEL.matcher(ua);
            if (matcher.find()) {
                String model = matcher.group(1).trim();
                if (StringUtils.isNotBlank(model) && !"Mobile".equalsIgnoreCase(model) && !"Linux".equalsIgnoreCase(model)) {
                    return model;
                }
            }
        }
        return null;
    }

    private static String findGroup(Pattern pattern, String ua) {
        Matcher matcher = pattern.matcher(ua);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String normalizeVersion(String version) {
        if (StringUtils.isBlank(version)) {
            return "";
        }
        return version.replace('_', '.').trim();
    }

    private static String mapWindowsVersion(String ntVersion) {
        if (StringUtils.isBlank(ntVersion)) {
            return "Windows";
        }
        switch (ntVersion) {
            case "10.0":
                return "Windows 10/11";
            case "6.3":
                return "Windows 8.1";
            case "6.2":
                return "Windows 8";
            case "6.1":
                return "Windows 7";
            default:
                return "Windows " + ntVersion;
        }
    }
}
