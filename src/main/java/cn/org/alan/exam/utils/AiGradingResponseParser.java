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
     * 解析「最终得分」展示分，保留小数（最多两位，与 {@link ExamScoreUtil} 一致）。
     */
    public static double parseFinalScore(JSONObject item) {
        if (item == null) {
            return 0D;
        }
        Object raw = item.get("最终得分");
        if (raw == null) {
            return 0D;
        }
        double value;
        if (raw instanceof Number) {
            value = ((Number) raw).doubleValue();
        } else {
            String s = String.valueOf(raw).trim();
            if (s.isEmpty()) {
                return 0D;
            }
            try {
                value = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0D;
            }
        }
        return ExamScoreUtil.toDisplay(ExamScoreUtil.toStorage(value)).doubleValue();
    }

    public static Integer parseQuestionId(JSONObject item) {
        if (item == null) {
            throw new IllegalArgumentException("评分项为空");
        }
        Object raw = item.get("题目ID");
        if (raw == null) {
            throw new IllegalArgumentException("评分结果缺少题目ID");
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("题目ID为空");
        }
        return Integer.valueOf(s);
    }
}
