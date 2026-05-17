package cn.org.alan.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 阅卷相关配置（密钥通过环境变量注入，勿写入可提交仓库的文件）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "online-exam.ai-grading")
public class AiGradingProperties {

    /**
     * 是否在阅卷前调用联网检索，将摘要作为「参考资料」注入 prompt。
     */
    private boolean webSearchEnabled = false;

    /**
     * 每题检索返回条数上限。
     */
    private int webSearchMaxResults = 3;
}
