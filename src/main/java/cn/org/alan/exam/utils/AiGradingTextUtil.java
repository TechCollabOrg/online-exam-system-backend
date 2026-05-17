package cn.org.alan.exam.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;

/**
 * AI 阅卷文本处理：去 HTML、空作答判断、分数钳制与原因格式化。
 */
public final class AiGradingTextUtil {

    public static final String AI_GRADING_TAG = "【AI阅卷】";
    private static final int MAX_REASON_LEN = 240;

    private AiGradingTextUtil() {
    }

    public static String stripHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean isBlankAnswer(String answer) {
        return StringUtils.isBlank(stripHtml(answer));
    }

    public static int clampScore(int score, Integer totalScore) {
        int max = totalScore != null && totalScore > 0 ? totalScore : Integer.MAX_VALUE;
        return Math.max(0, Math.min(score, max));
    }

    public static String formatAiReason(String rawReason) {
        String body = StringUtils.isBlank(rawReason) ? "已按要点完成评分。" : rawReason.trim();
        if (body.startsWith(AI_GRADING_TAG)) {
            return truncate(body);
        }
        return truncate(AI_GRADING_TAG + " " + body);
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_REASON_LEN) {
            return text;
        }
        return text.substring(0, MAX_REASON_LEN - 1) + "…";
    }
}
