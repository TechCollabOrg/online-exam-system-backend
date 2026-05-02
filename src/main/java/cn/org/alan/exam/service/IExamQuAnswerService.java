package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.exam.ExamQuAnswerExtVO;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;

/**
 * 考试作答明细：用户对各题的作答内容，及按题维度的答题分析查询。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IExamQuAnswerService extends IService<ExamQuAnswer> {

    /**
     * 获取某场考试某题作答情况
     * @param examId 考试id
     * @param questionId 试题id
     * @return 结果
     */
    Result<QuestionAnalyseVO> questionAnalyse(Integer examId, Integer questionId);

}
