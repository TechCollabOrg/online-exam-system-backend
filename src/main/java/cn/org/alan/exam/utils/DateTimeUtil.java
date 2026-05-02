package cn.org.alan.exam.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式化与 {@link LocalDateTime}/{@link LocalDate} 解析工具（静态方法集合）。
 *
 * @author WeiJin
 */
public class DateTimeUtil {

    /** 工具类禁止实例化 */
    private DateTimeUtil() {
    }
    private static String dataFormat = "yyyy-MM-dd";
    private static String format = "yyyy-MM-dd HH:mm:ss";

    /**
     * 获取当前时刻并按 {@code yyyy-MM-dd HH:mm:ss} 格式Round-trip 解析后的 {@link LocalDateTime}（与字符串格式保持一致）。
     *
     * @return 当前时间
     */
    public static LocalDateTime getDateTime() {
        return LocalDateTime.parse(datetimeToStr(LocalDateTime.now()), DateTimeFormatter.ofPattern(format));
    }

    /**
     * 获取当前日期并按 {@code yyyy-MM-dd} 格式 Round-trip 后的 {@link LocalDate}。
     *
     * @return 当前日期
     */
    public static LocalDate getDate() {
        return LocalDate.parse(dateToStr(LocalDate.now()), DateTimeFormatter.ofPattern(dataFormat));
    }

    /**
     * 将 {@link LocalDateTime} 格式化为 {@code yyyy-MM-dd HH:mm:ss} 字符串。
     *
     * @param dateTime 待格式化时间
     * @return 格式化字符串
     */
    public static String datetimeToStr(LocalDateTime dateTime) {
        return DateTimeFormatter.ofPattern(format).format(dateTime);

    }

    /**
     * 将 {@link LocalDate} 格式化为 {@code yyyy-MM-dd} 字符串。
     *
     * @param date 待格式化日期
     * @return 格式化字符串
     */
    public static String dateToStr(LocalDate date) {
        return DateTimeFormatter.ofPattern(dataFormat).format(date);

    }

}
