package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.ExamQuAnswer;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 主观题等场景的自动评分入口：按考试与用户触发打分流水线（如调用大模型）。
 *
 * @author 赵浩森
 * @since 2025/4/12
 */
public interface IAutoScoringService extends IService<ExamQuAnswer> {
    /**
     * 自动评分服务
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 评分结果列表
     */
    void autoScoringExam(Integer examId, Integer userId);
} 