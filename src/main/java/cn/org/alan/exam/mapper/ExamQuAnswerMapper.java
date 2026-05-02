package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.question.QuestionScoreVO;
import cn.org.alan.exam.model.vo.answer.UserAnswerDetailVO;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试中用户对各题的作答结果 Mapper：单题解析、主观题阅卷明细、待评分题目列表。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface ExamQuAnswerMapper extends BaseMapper<ExamQuAnswer> {

    /**
     * 获取单题作答信息
     *
     * @param examId     考试id
     * @param questionId 试题id
     * @return 查询结果
     */
    QuestionAnalyseVO questionAnalyse(Integer examId, Integer questionId);

    /**
     * 获取用户回答主观题信息
     *
     * @param userId 用户id
     * @param examId 考试id
     * @return 结果集
     */
    List<UserAnswerDetailVO> selectUserAnswer(Integer userId, Integer examId);

    /**
     * 某场考试中指定考生尚未完成阅卷打分的题目列表。
     *
     * @param examId 考试 ID
     * @param userId 考生用户 ID
     */
    List<QuestionScoreVO> getQuestionsForGrading(Integer examId, Integer userId);
}
