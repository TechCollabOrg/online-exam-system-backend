package cn.org.alan.exam.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从大模型回复中解析 AI 阅卷 JSON。
 */
public final class AiGradingResponseParser {

    private static final Pattern JSON_FENCE = Pattern.compile(
            "```json\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private AiGradingResponseParser() {
    }

    public static JSONArray parseScoreItems(String modelResponse) {
        if (StringUtils.isBlank(modelResponse)) {
            return null;
        }
        String jsonText = extractJsonText(modelResponse.trim());
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        JSONObject root = JSONUtil.parseObj(jsonText);
        JSONArray arr = root.getJSONArray("评分结果");
        return arr == null || arr.isEmpty() ? null : arr;
    }

    private static String extractJsonText(String response) {
        Matcher matcher = JSON_FENCE.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        return null;
    }

    /**
     * 解析「最终得分」，支持整数与小数。
     */
    public static int parseFinalScore(JSONObject item) {
        if (item == null) {
            return 0;
        }
        Object raw = item.get("最终得分");
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number) {
            return (int) Math.round(((Number) raw).doubleValue());
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return 0;
        }
        return (int) Math.round(Double.parseDouble(s));
    }
}
