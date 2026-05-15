package cn.org.alan.exam.task;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.*;
import cn.org.alan.exam.model.entity.*;
import cn.org.alan.exam.model.enums.ExamState;
import cn.org.alan.exam.model.vo.exam.ExamQuDetailVO;
import cn.org.alan.exam.service.IAutoScoringService;
import cn.org.alan.exam.utils.ClassTokenGenerator;
import cn.org.alan.exam.utils.ExamGradingUtil;
import cn.org.alan.exam.utils.QuestionSubItemsUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考试相关定时任务：扫描「进行中」的 {@link UserExamsScore}，在用户答题时长超过试卷限定时间后触发自动交卷。
 * <p>
 * 结束时刻 = 用户开始答题时间（记录创建时间）+ 试卷 {@link Exam#getExamDuration()} 分钟；与全局 {@link Exam#getEndTime()} 的注释代码分支二选一，
 * 当前启用的是「个人开考时刻 + 时长」模型。
 * </p>
 */
@Component
@Slf4j
public class ExamTask {
    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private UserBookMapper userBookMapper;
    @Resource
    private ExamQuestionMapper examQuestionMapper;
    @Resource
    private CertificateUserMapper certificateUserMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private IAutoScoringService autoScoringService;

    /**
     * 每 5 秒执行（启动后延迟 1 秒）：查询状态为进行中的用户考试记录，若当前时间已超过该用户本场考试的截止时间则调用 {@link #handExam(UserExamsScore)}。
     */
    @Scheduled(initialDelay = 1000, fixedDelay = 5 * 1000)
    public void test() {
        // 查询出正在考试的用户信息
        LambdaQueryWrapper<UserExamsScore> query = new LambdaQueryWrapper<>();
        query.eq(UserExamsScore::getState, ExamState.ONGOING.getCode());
        List<UserExamsScore> userExamsScores = userExamsScoreMapper.selectList(query);
        //  获取当前时间
        LocalDateTime now = LocalDateTime.now();
//         if(userExamsScores.size()>0){
//             for (UserExamsScore userExamsScore : userExamsScores) {
//                 try {
//                     // 查找到具体考试到信息
//                     Integer examId = userExamsScore.getExamId();
//                     Exam exam = examMapper.selectById(examId);
//                     // 3. 获取考试信息
//                     if(exam == null) {
//                         log.error("考试不存在，examId: {}", exam.getId());
//                         continue;
//                     }
//
//                     LocalDateTime endTime = exam.getEndTime();
//                     // 4. 检查是否超时
//                     if(now.isAfter(exam.getEndTime())) {
//                         // 5. 调用交卷函数
//                         handExam(userExamsScore);
//
//                         log.info("自动交卷成功，用户ID: {}, 考试ID: {}",
//                                 userExamsScore.getUserId(), userExamsScore.getExamId());
//                     }
//                 } catch (Exception e) {
//                     log.error("自动交卷处理异常，用户考试记录ID: {}", userExamsScore.getId(), e);
//                 }
//             }
//         }
        for (UserExamsScore userExamsScore : userExamsScores) {
            try {
                // 查找到具体考试的信息
                Integer examId = userExamsScore.getExamId();
                Exam exam = examMapper.selectById(examId);

                if (exam == null) {
                    log.error("考试不存在，examId: {}", examId);
                    continue;
                }

                // 计算考试结束时间
                LocalDateTime userStartTime = userExamsScore.getCreateTime();//获取用户实际的开始时间 (UserExamsScore 记录的创建时间
                if (userStartTime == null) {
                    // 如果因为某些原因没取到 userExamsScore 或者其 createTime 为 null，需要处理
                    // 可以尝试重新从数据库获取一次该记录确保拿到最新数据
                    UserExamsScore currentRecord = userExamsScoreMapper.selectById(userExamsScore.getId());
                    if (currentRecord == null || currentRecord.getCreateTime() == null) {
                        log.error("无法获取用户考试记录的实际开始时间，用户考试记录ID: {}", userExamsScore.getId());
                        continue; // 跳过此记录
                    }
                    userStartTime = currentRecord.getCreateTime();
                }

                LocalDateTime userEndTime = userStartTime.plusMinutes(exam.getExamDuration());

                // 检查是否超时
                if (now.isAfter(userEndTime)) {
                    // 调用交卷函数
                    handExam(userExamsScore);
                    log.info("自动交卷成功，用户ID: {}, 考试ID: {}",
                            userExamsScore.getUserId(), userExamsScore.getExamId());
                }
            } catch (Exception e) {
                log.error("自动交卷处理异常，用户考试记录ID: {}", userExamsScore.getId(), e);
            }
        }
    }

    /**
     * 执行交卷：汇总客观题得分、错题入库、更新 {@link UserExamsScore} 状态与用时；若含简答题则触发自动阅卷或等待教师阅卷；
     * 纯客观题且达及格线可发放证书关联记录。
     *
     * @param ues 用户本场考试得分记录（进行中状态）
     * @return 成功提示；含简答题时可能为「待老师阅卷」
     */
    @Transactional
    public Result<ExamQuDetailVO> handExam(UserExamsScore ues) {
        // 获取当前时间

        LocalDateTime nowTime = LocalDateTime.now();
        // 查询考试表记录
        Exam examOne = examMapper.selectById(ues.getExamId());
        // 设置考试状态
        UserExamsScore userExamsScore = new UserExamsScore();
        userExamsScore.setUserScore(0);
        userExamsScore.setState(1);

        insertUnansweredSubjectiveAnswers(ues.getExamId(), ues.getUserId());

        // 查询用户答题记录
        LambdaQueryWrapper<ExamQuAnswer> examQuAnswerLambdaQuery = new LambdaQueryWrapper<>();
        examQuAnswerLambdaQuery.eq(ExamQuAnswer::getUserId, ues.getUserId())
                .eq(ExamQuAnswer::getExamId, ues.getExamId());
        List<ExamQuAnswer> examQuAnswer = examQuAnswerMapper.selectList(examQuAnswerLambdaQuery);
        Map<Integer, Integer> questionScoreMap = loadExamQuestionScoreMap(ues.getExamId());
        // 客观分
        List<UserBook> userBookArrayList = new ArrayList<>();
        boolean hasManualReview = false;
        for (ExamQuAnswer temp : examQuAnswer) {
            if (temp.getIsRight() != null && temp.getIsRight() == 1) {
                Integer earned = resolveEarnedScore(questionScoreMap, examOne, temp.getQuestionId(), temp.getQuestionType());
                if (earned != null) {
                    userExamsScore.setUserScore(userExamsScore.getUserScore() + earned);
                }
            } else if (temp.getIsRight() != null && temp.getIsRight() == -1) {
                // 简答题或含简答子题的复合题，待人工阅卷，不加入错题本
                hasManualReview = true;
            } else if (temp.getIsRight() == 0) {
                UserBook userBook = new UserBook();
                userBook.setExamId(ues.getExamId());
                userBook.setUserId(ues.getUserId());
                userBook.setQuId(temp.getQuestionId());
                userBook.setCreateTime(nowTime);
                userBookArrayList.add(userBook);
            }
        }
        if (!userBookArrayList.isEmpty()) {
            // 把打错的问题加入错题本
            userBookMapper.addUserBookList(userBookArrayList);
        }
        // 设置用户用时和提交试卷
        userExamsScore.setLimitTime(nowTime);
        // 开始时间
        LambdaQueryWrapper<UserExamsScore> userExamsScoreLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userExamsScoreLambdaQueryWrapper.eq(UserExamsScore::getUserId, ues.getUserId())
                .eq(UserExamsScore::getExamId, ues.getExamId());
        UserExamsScore userExamsScore1 = userExamsScoreMapper.selectOne(userExamsScoreLambdaQueryWrapper);
        LocalDateTime createTime = userExamsScore1.getCreateTime();
        long secondsDifference = Duration.between(createTime, nowTime).getSeconds();
        int differenceAsInteger = (int) secondsDifference;
        // 检查是否在Integer范围内
        // if (secondsDifference <= Integer.MAX_VALUE && secondsDifference >= Integer.MIN_VALUE)
        userExamsScore.setUserTime(differenceAsInteger);
        // 添加总分和状态
        LambdaUpdateWrapper<UserExamsScore> userExamsScoreLambdaUpdate = new LambdaUpdateWrapper<>();
        userExamsScoreLambdaUpdate.eq(UserExamsScore::getUserId, ues.getUserId())
                .eq(UserExamsScore::getExamId, ues.getExamId());
        userExamsScoreMapper.update(userExamsScore, userExamsScoreLambdaUpdate);
        if (ExamGradingUtil.examPaperNeedsManualGrading(ues.getExamId(), examQuestionMapper, questionMapper)
                || hasManualReview) {
            LambdaUpdateWrapper<UserExamsScore> userExamsScoreLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            userExamsScoreLambdaUpdateWrapper.set(UserExamsScore::getWhetherMark, 0)
                    .eq(UserExamsScore::getExamId, ues.getExamId())
                    .eq(UserExamsScore::getUserId, ues.getUserId());
            userExamsScoreMapper.update(userExamsScoreLambdaUpdateWrapper);
            autoScoringService.autoScoringExam(ues.getExamId(), ues.getUserId());
            return Result.success("提交成功，待老师阅卷");
        }
        if (userExamsScore.getUserScore() >= examOne.getPassedScore()) {
            CertificateUser certificateUser = new CertificateUser();
            certificateUser.setCertificateId(examOne.getCertificateId());
            certificateUser.setUserId(ues.getUserId());
            certificateUser.setExamId(ues.getExamId());
            certificateUser.setCode(ClassTokenGenerator.generateClassToken(18));
            certificateUserMapper.insert(certificateUser);
        }
        // 查询有简答题是否回答
        Exam byId = examMapper.selectById(ues.getExamId());
        if (byId.getSaqCount() > 0) {
            LambdaQueryWrapper<ExamQuAnswer> examQuAnswerLambdaQueryWrapper = new LambdaQueryWrapper<>();
            examQuAnswerLambdaQueryWrapper.eq(ExamQuAnswer::getUserId, ues.getUserId())
                    .eq(ExamQuAnswer::getExamId, ues.getExamId())
                    .eq(ExamQuAnswer::getQuestionType, 4);
            List<ExamQuAnswer> examQuAnswers = examQuAnswerMapper.selectList(examQuAnswerLambdaQueryWrapper);
            if (examQuAnswers.isEmpty()) {
                LambdaQueryWrapper<ExamQuestion> examQuestionLambdaQueryWrapper = new LambdaQueryWrapper<>();
                examQuestionLambdaQueryWrapper.eq(ExamQuestion::getExamId, ues.getExamId())
                        .eq(ExamQuestion::getType, 4);
                List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionLambdaQueryWrapper);
                examQuestions.forEach(temp -> {
                    ExamQuAnswer examQuAnswer1 = new ExamQuAnswer();
                    examQuAnswer1.setExamId(ues.getExamId());
                    examQuAnswer1.setUserId(ues.getUserId());
                    examQuAnswer1.setQuestionId(temp.getQuestionId());
                    examQuAnswer1.setQuestionType(temp.getType());
                    examQuAnswer1.setIsRight(-1);
                    examQuAnswerMapper.insert(examQuAnswer1);
                });
            }

        }

        LambdaUpdateWrapper<UserExamsScore> userExamsScoreLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        userExamsScoreLambdaUpdateWrapper.set(UserExamsScore::getWhetherMark, -1)
                .eq(UserExamsScore::getExamId, ues.getExamId())
                .eq(UserExamsScore::getUserId, ues.getUserId());
        userExamsScoreMapper.update(userExamsScoreLambdaUpdateWrapper);
        return Result.success("交卷成功");
    }

    /** 自动交卷前补录未作答的简答题/复合题，避免阅卷列表漏题。 */
    private void insertUnansweredSubjectiveAnswers(Integer examId, Integer userId) {
        List<ExamQuestion> unansweredSaq = examQuestionMapper.getUnansweredSaqQuestions(examId, userId);
        if (unansweredSaq != null) {
            for (ExamQuestion question : unansweredSaq) {
                ExamQuAnswer row = new ExamQuAnswer();
                row.setExamId(examId);
                row.setUserId(userId);
                row.setQuestionId(question.getQuestionId());
                row.setQuestionType(4);
                row.setAnswerContent("");
                row.setIsRight(0);
                examQuAnswerMapper.insert(row);
            }
        }
        List<ExamQuestion> unansweredCompound = examQuestionMapper.getUnansweredCompoundQuestions(examId, userId);
        if (unansweredCompound != null) {
            for (ExamQuestion question : unansweredCompound) {
                Question compoundQu = questionMapper.selectById(question.getQuestionId());
                ExamQuAnswer row = new ExamQuAnswer();
                row.setExamId(examId);
                row.setUserId(userId);
                row.setQuestionId(question.getQuestionId());
                row.setQuestionType(5);
                row.setAnswerContent("{}");
                row.setIsRight(gradeCompoundForHandExam(compoundQu, "{}"));
                examQuAnswerMapper.insert(row);
            }
        }
    }

    private int gradeCompoundForHandExam(Question question, String answerJson) {
        if (question == null || !Integer.valueOf(5).equals(question.getQuType())) {
            return 0;
        }
        List<cn.org.alan.exam.model.form.question.QuestionSubItemForm> subItems =
                QuestionSubItemsUtil.parseForms(question.getSubItems());
        if (subItems.isEmpty()) {
            return 0;
        }
        for (cn.org.alan.exam.model.form.question.QuestionSubItemForm sub : subItems) {
            if (sub != null && Integer.valueOf(4).equals(sub.getQuType())) {
                return -1;
            }
        }
        return 0;
    }

    private Map<Integer, Integer> loadExamQuestionScoreMap(Integer examId) {
        LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamQuestion::getExamId, examId);
        List<ExamQuestion> list = examQuestionMapper.selectList(wrapper);
        Map<Integer, Integer> map = new HashMap<>();
        for (ExamQuestion eq : list) {
            if (eq.getQuestionId() != null && eq.getScore() != null) {
                map.put(eq.getQuestionId(), eq.getScore());
            }
        }
        return map;
    }

    private Integer resolveEarnedScore(Map<Integer, Integer> questionScoreMap, Exam exam, Integer questionId, Integer questionType) {
        if (questionId != null && questionScoreMap.containsKey(questionId)) {
            return questionScoreMap.get(questionId);
        }
        if (questionType == null || exam == null) {
            return null;
        }
        if (questionType == 1) {
            return exam.getRadioScore();
        }
        if (questionType == 2) {
            return exam.getMultiScore();
        }
        if (questionType == 3) {
            return exam.getJudgeScore();
        }
        if (questionType == 4 || questionType == 5) {
            return exam.getSaqScore();
        }
        return null;
    }

}