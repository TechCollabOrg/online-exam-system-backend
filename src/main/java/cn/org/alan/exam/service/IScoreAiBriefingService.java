package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.score.ScoreAiBriefingVO;

/**
 * 成绩分析 AI 自然语言简报。
 */
public interface IScoreAiBriefingService {

    /**
     * 根据考试与班级统计数据生成 AI 简报。
     */
    Result<ScoreAiBriefingVO> generateBriefing(Integer examId, Integer gradeId);
}
