package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;
import cn.org.alan.exam.service.IExamQuAnswerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 考试作答明细 {@link ExamQuAnswer}：扩展「单题正确率分析」等阅卷统计能力。
 *
 * @author WeiJin
 */
@Service
public class ExamQuAnswerServiceImpl extends ServiceImpl<ExamQuAnswerMapper, ExamQuAnswer> implements IExamQuAnswerService {

    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;

    /**
     * 统计某场考试指定题目的作答人数、正确数，并格式化为两位小数正确率。
     */
    @Override
    public Result<QuestionAnalyseVO> questionAnalyse(Integer examId, Integer questionId) {
        QuestionAnalyseVO questionAnalyseVO = examQuAnswerMapper.questionAnalyse(examId, questionId);
        //正确率保留两位小数
        DecimalFormat format = new DecimalFormat("#.00");
        String strAccuracy = format.format(questionAnalyseVO.getRightCount() / questionAnalyseVO.getTotalCount());
        questionAnalyseVO.setAccuracy(Double.parseDouble(strAccuracy));
        return Result.success(null, questionAnalyseVO);
    }

}
