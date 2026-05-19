package cn.org.alan.exam.service;

/**
 * 为 AI 阅卷提供联网检索摘要（可选，需配置检索 API 密钥）。
 */
public interface IAiGradingWebSearchService {

    /**
     * 根据题干文本检索外部资料摘要，供大模型阅卷参考。
     *
     * @param questionPlainText 已去除 HTML 的题干
     * @return 参考资料文本；未启用或无密钥时返回空字符串
     */
    String searchReference(String questionPlainText);
}
