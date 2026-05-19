package cn.org.alan.exam.service;

/**
 * AI 助手知识库检索：仅包含系统操作说明，不含题库与答卷数据。
 */
public interface AiKnowledgeRagService {

    /**
     * 根据用户问题检索相关知识片段，供拼入系统提示词。
     */
    String retrieveContext(String query);
}
