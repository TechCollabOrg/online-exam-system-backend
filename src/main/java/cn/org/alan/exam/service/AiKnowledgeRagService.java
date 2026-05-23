package cn.org.alan.exam.service;

/**
 * AI 助手知识库检索：内容来自管理员维护的 {@code t_ai_knowledge_doc}，不含题库与答卷数据。
 */
public interface AiKnowledgeRagService {

    /**
     * 根据用户问题检索相关知识片段，供拼入系统提示词。
     */
    String retrieveContext(String query);

    /**
     * 从数据库重新加载并分片（增删改文档后调用）。
     */
    void reloadIndex();
}
