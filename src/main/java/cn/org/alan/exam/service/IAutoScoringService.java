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
     * 异步自动评分（交卷后定时任务等场景）。
     */
    void autoScoringExam(Integer examId, Integer userId);

    /**
     * 同步 AI 阅卷（教师手动触发）：逐题评分并写库，返回成功题数。
     */
    int autoScoringExamSync(Integer examId, Integer userId);
}
