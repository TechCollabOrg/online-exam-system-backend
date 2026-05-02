package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.*;
import cn.org.alan.exam.model.entity.*;
import cn.org.alan.exam.model.form.answer.CorrectAnswerFrom;
import cn.org.alan.exam.model.vo.answer.AnswerExamVO;
import cn.org.alan.exam.model.vo.answer.UncorrectedUserVO;
import cn.org.alan.exam.model.vo.answer.UserAnswerDetailVO;
import cn.org.alan.exam.service.IManualScoreService;
import cn.org.alan.exam.utils.ClassTokenGenerator;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 简答题/主观题人工阅卷：查看考生作答明细、批量打分写 {@link ManualScore}、累加总分并标记已阅卷，
 * 及格且考试绑定证书时写入 {@link CertificateUser}；教师侧待阅试卷分页与待阅学生分页。
 *
 * @author WeiJin
 */
@Service
public class ManualScoreServiceImpl extends ServiceImpl<ManualScoreMapper, ManualScore> implements IManualScoreService {

    @Resource
    private ExamMapper examMapper;
    @Resource
    private ExamGradeMapper examGradeMapper;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;
    @Resource
    private ManualScoreMapper manualScoreMapper;
    @Resource
    private CertificateUserMapper certificateUserMapper;

    /** 教师查看指定学员在某场考试中的主观题作答明细列表。 */
    @Override
    public Result<List<UserAnswerDetailVO>> getDetail(Integer userId, Integer examId) {
        List<UserAnswerDetailVO> list = examQuAnswerMapper.selectUserAnswer(userId, examId);
        return Result.success("查询成功", list);
    }

    /**
     * 批量提交批改分数：插入 manual_score、更新 user_exams_score 总分与 whetherMark，并按及格线发证书。
     */
    @Override
    @Transactional
    public Result<String> correct(List<CorrectAnswerFrom> correctAnswerFroms) {
        List<ManualScore> list = new ArrayList<>(correctAnswerFroms.size());
        AtomicInteger manualTotalScore = new AtomicInteger();
        correctAnswerFroms.forEach(correctAnswerFrom -> {

            // 获取用户作答信息id
            LambdaQueryWrapper<ExamQuAnswer> wrapper = new LambdaQueryWrapper<ExamQuAnswer>()
                    .select(ExamQuAnswer::getId)
                    .eq(ExamQuAnswer::getExamId, correctAnswerFrom.getExamId())
                    .eq(ExamQuAnswer::getUserId, correctAnswerFrom.getUserId())
                    .eq(ExamQuAnswer::getQuestionId, correctAnswerFrom.getQuestionId());

            ManualScore manualScore = new ManualScore();
            manualScore.setExamQuAnswerId(examQuAnswerMapper.selectOne(wrapper).getId());
            manualScore.setScore(correctAnswerFrom.getScore());
            list.add(manualScore);
            manualTotalScore.addAndGet(correctAnswerFrom.getScore());
        });
        manualScoreMapper.insertList(list);

        // 把用户考试记录修改为已批改，并把简答题分数添加进去
        CorrectAnswerFrom correctAnswerFrom = correctAnswerFroms.get(0);
        LambdaUpdateWrapper<UserExamsScore> userExamsScoreLambdaUpdateWrapper = new LambdaUpdateWrapper<UserExamsScore>()
                .eq(UserExamsScore::getExamId, correctAnswerFrom.getExamId())
                .eq(UserExamsScore::getUserId, correctAnswerFrom.getUserId())
                .set(UserExamsScore::getWhetherMark, 1)
                .setSql("user_score = user_score + " + manualTotalScore.get());
        userExamsScoreMapper.update(userExamsScoreLambdaUpdateWrapper);

        // 根据该考试是否有证书来给用户颁发对应证书
        // 判断该考试是否有证书
        LambdaQueryWrapper<Exam> examWrapper = new LambdaQueryWrapper<Exam>()
                .select(Exam::getId, Exam::getCertificateId, Exam::getPassedScore)
                .eq(Exam::getId, correctAnswerFrom.getExamId());
        Exam exam = examMapper.selectOne(examWrapper);
        // 不必对exam做非空验证，这里一定不为null
        if (exam.getCertificateId() != null && exam.getCertificateId() > 0) {
            // 有证书 获取用户得分
            LambdaQueryWrapper<UserExamsScore> examsScoreWrapper = new LambdaQueryWrapper<UserExamsScore>()
                    .select(UserExamsScore::getId, UserExamsScore::getUserScore)
                    .eq(UserExamsScore::getExamId, correctAnswerFrom.getExamId())
                    .eq(UserExamsScore::getUserId, correctAnswerFrom.getUserId());
            UserExamsScore userExamsScore = userExamsScoreMapper.selectOne(examsScoreWrapper);
            // 不必对userExamsScore做非空验证，这里一定不为null
            if (userExamsScore.getUserScore() >= exam.getPassedScore()) {
                // 分数合格，判罚证书
                CertificateUser certificateUser = new CertificateUser();
                certificateUser.setUserId(correctAnswerFrom.getUserId());
                certificateUser.setExamId(correctAnswerFrom.getExamId());
                certificateUser.setCode(ClassTokenGenerator.generateClassToken(18));
                certificateUser.setCertificateId(exam.getCertificateId());
                certificateUserMapper.insert(certificateUser);
            }

        }
        return Result.success("批改成功");
    }

    /** 当前教师创建的、包含待阅主观题的考试分页，补充班级应考人数、实考人数、已阅份数。 */
    @Override
    public Result<IPage<AnswerExamVO>> examPage(Integer pageNum, Integer pageSize, String examName) {

        Page<AnswerExamVO> page = new Page<>(pageNum, pageSize);
        // 获取自己创建的考试
        List<AnswerExamVO> list = examMapper.selectMarkedList(page, SecurityUtil.getUserId(), SecurityUtil.getRole(), examName).getRecords();

        // 获取相关信息
        list.forEach(answerExamVO -> {
            // 需要参加考试人数
            answerExamVO.setClassSize(examGradeMapper.selectClassSize(answerExamVO.getExamId()));
            // 实际参加考试人数
            LambdaQueryWrapper<UserExamsScore> numberWrapper = new LambdaQueryWrapper<UserExamsScore>()
                    .eq(UserExamsScore::getExamId, answerExamVO.getExamId());
            answerExamVO.setNumberOfApplicants(userExamsScoreMapper.selectCount(numberWrapper).intValue());
            // 已阅人数
            LambdaQueryWrapper<UserExamsScore> correctedWrapper = new LambdaQueryWrapper<UserExamsScore>()
                    .eq(UserExamsScore::getWhetherMark, 1)
                    .eq(UserExamsScore::getExamId, answerExamVO.getExamId());
            answerExamVO.setCorrectedPaper(userExamsScoreMapper.selectCount(correctedWrapper).intValue());
        });
        return Result.success(null, page);

    }

    /** 某场考试下仍未完成阅卷（whetherMark 未置完成）的考生分页，支持姓名模糊。 */
    @Override
    public Result<IPage<UncorrectedUserVO>> stuExamPage(Integer pageNum, Integer pageSize, Integer examId, String realName) {
        IPage<UncorrectedUserVO> page = new Page<>(pageNum, pageSize);
        page = userExamsScoreMapper.uncorrectedUser(page, examId, realName);
        return Result.success(null, page);
    }
}
