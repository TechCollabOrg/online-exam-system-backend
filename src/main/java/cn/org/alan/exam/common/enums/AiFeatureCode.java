package cn.org.alan.exam.common.enums;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;

/**
 * AI 分功能配置编码（与 t_ai_feature_config.feature_code 一致）。
 */
public enum AiFeatureCode {

    GRADING("grading", "AI 阅卷"),
    ASSISTANT("assistant", "AI 助手"),
    BRIEFING("briefing", "成绩简报"),
    QUESTION_REVIEW("question_review", "考后单题解析"),
    QUESTION_IMPORT("question_import", "AI 试题导入");

    private final String code;
    private final String label;

    AiFeatureCode(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static AiFeatureCode fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (AiFeatureCode value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        return null;
    }
}
