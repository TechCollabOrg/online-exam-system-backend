package cn.org.alan.exam.model.dto;

import lombok.Data;

/**
 * 运行时解析出的 LLM 连接参数。
 */
@Data
public class LlmResolvedConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    /** 是否来自数据库管理员配置 */
    private boolean fromDatabase;

    public static LlmResolvedConfig of(String baseUrl, String apiKey, String modelName, boolean fromDatabase) {
        LlmResolvedConfig c = new LlmResolvedConfig();
        c.setBaseUrl(baseUrl);
        c.setApiKey(apiKey);
        c.setModelName(modelName);
        c.setFromDatabase(fromDatabase);
        return c;
    }
}
