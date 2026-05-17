package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.UserExamsScore;
import cn.org.alan.exam.model.vo.answer.UncorrectedUserVO;
import cn.org.alan.exam.model.vo.score.ExportScoreVO;
import cn.org.alan.exam.model.vo.score.GradeScoreVO;
import cn.org.alan.exam.model.vo.score.ScoreBriefingRowVO;
import cn.org.alan.exam.model.vo.score.MyExamScoreRow;
import cn.org.alan.exam.model.vo.score.PeerExamScoreRow;
import cn.org.alan.exam.model.vo.score.UserScoreVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户考试成绩表 Mapper：班级成绩统计分页、成绩分页、导出列表、缺考/未阅卷用户分页。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface UserExamsScoreMapper extends BaseMapper<UserExamsScore> {

    /**
     * 考试班级用户成绩分析
     *
     * @param page        分页信息
     * @param gradeId     班级Id
     * @param examTitle   考试名称
     * @param userId      用户Id
     * @param roleId      角色Id
     * @param gradeIdList 班级ID集合
     * @return 结果
     */
    IPage<GradeScoreVO> scoreStatistics(IPage<GradeScoreVO> page, Integer gradeId, String examTitle, Integer userId,
                                        Integer roleId, List<Integer> gradeIdList);

    /**
     * 成绩分页查询
     *
     * @param page     分页信息
     * @param gradeId  班级Id
     * @param examId   考试Id
     * @param realName 真实姓名
     * @return 查询结果集
     */
    IPage<UserScoreVO> pagingScore(IPage<UserScoreVO> page, Integer gradeId, Integer examId, String realName);

    /**
     * 获取成绩
     *
     * @param examId  考试id
     * @param gradeId 班级id
     * @return 查询结果
     */
    List<ExportScoreVO> selectScores(Integer examId, Integer gradeId);

    /**
     * 某班某场考试已出分成绩（含切屏次数），供 AI 简报统计。
     */
    List<ScoreBriefingRowVO> listBriefingScores(@Param("examId") Integer examId, @Param("gradeId") Integer gradeId);

    /**
     * 根据考试id获取未考试用户
     *
     * @param page   分页信息
     * @param examId 考试id
     * @return 查询结果
     */
    IPage<UncorrectedUserVO> uncorrectedUser(IPage<UncorrectedUserVO> page, Integer examId, String realName);

    /**
     * 当前学生已交卷且已出分（含无简答题）的考试记录，按交卷时间升序。
     */
    List<MyExamScoreRow> listMyCompletedExamScores(@Param("userId") Integer userId);

    /**
     * 指定班级在若干场考试下的所有学生得分（用于计算班级名次）。
     */
    List<PeerExamScoreRow> listClassScoresForExams(@Param("gradeId") Integer gradeId, @Param("examIds") List<Integer> examIds);

}
