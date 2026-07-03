package cn.org.alan.exam.utils.question;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 AI 回复中提取 JSON 文本（兼容 ```json 代码块包裹）。
 */
public final class AiJsonResponseExtractor {

    private static final Pattern FENCED_JSON = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private AiJsonResponseExtractor() {
    }

    public static String extract(String aiResponse) {
        if (StringUtils.isBlank(aiResponse)) {
            throw new ServiceRuntimeException("AI 未返回有效内容");
        }
        String text = aiResponse.trim();
        Matcher matcher = FENCED_JSON.matcher(text);
        if (matcher.find()) {
            text = matcher.group(1).trim();
        }
        int start = findJsonStart(text);
        if (start < 0) {
            throw new ServiceRuntimeException("AI 返回内容中未找到 JSON 结构");
        }
        int end = findJsonEnd(text, start);
        if (end <= start) {
            throw new ServiceRuntimeException("AI 返回的 JSON 不完整");
        }
        return text.substring(start, end + 1).trim();
    }

    private static int findJsonStart(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                return i;
            }
        }
        return -1;
    }

    private static int findJsonEnd(String text, int start) {
        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
