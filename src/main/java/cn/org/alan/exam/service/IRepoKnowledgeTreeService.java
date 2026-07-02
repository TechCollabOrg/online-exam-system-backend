package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgePointOptionVO;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgeTreeVO;

import java.util.List;

/**
 * 题库知识树：基于 AI 分析题目生成知识点层级结构。
 */
public interface IRepoKnowledgeTreeService {

    /**
     * 获取已保存的知识树（未生成时返回空结构）。
     */
    Result<RepoKnowledgeTreeVO> getKnowledgeTree(Integer repoId);

    /**
     * 调用 AI 分析题库题目并生成/覆盖知识树。
     */
    Result<RepoKnowledgeTreeVO> generateKnowledgeTree(Integer repoId);

    /**
     * 获取知识树下拉选项（扁平列表，含路径与题目数）。
     */
    Result<List<RepoKnowledgePointOptionVO>> listKnowledgePointOptions(Integer repoId);

    /**
     * 按知识点路径解析关联题目 ID（含子节点题目）。
     */
    List<Integer> resolveQuestionIds(Integer repoId, String knowledgePointPath);
}
