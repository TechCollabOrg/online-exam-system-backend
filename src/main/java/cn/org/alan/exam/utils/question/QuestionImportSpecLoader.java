package cn.org.alan.exam.utils.question;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 加载试题 JSON 导入规范（与 sql/JSON_QUESTION_IMPORT_SPEC.md 同步）。
 */
public final class QuestionImportSpecLoader {

    private static final String SPEC_PATH = "ai/JSON_QUESTION_IMPORT_SPEC.md";

    private QuestionImportSpecLoader() {
    }

    public static String loadSpecText() {
        try {
            ClassPathResource resource = new ClassPathResource(SPEC_PATH);
            if (!resource.exists()) {
                throw new ServiceRuntimeException("未找到试题导入规范文件：" + SPEC_PATH);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServiceRuntimeException("读取试题导入规范失败：" + e.getMessage());
        }
    }

    public static String buildAiSystemPrompt() {
        return "你是试题 JSON 生成器。严格按下方《JSON_QUESTION_IMPORT_SPEC.md》输出 UTF-8 合法 JSON。\n"
                + "要求：只输出 JSON，不要 Markdown 代码块，不要解释文字。\n\n"
                + loadSpecText();
    }
}
