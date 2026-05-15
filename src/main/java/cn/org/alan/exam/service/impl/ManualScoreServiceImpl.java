package cn.org.alan.exam.service.impl;



import cn.org.alan.exam.common.exception.ServiceRuntimeException;

import cn.org.alan.exam.common.result.Result;

import cn.org.alan.exam.mapper.*;

import cn.org.alan.exam.model.entity.*;

import cn.org.alan.exam.model.form.answer.CorrectAnswerFrom;

import cn.org.alan.exam.model.form.question.QuestionSubItemForm;

import cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm;

import cn.org.alan.exam.model.vo.answer.AnswerExamVO;

import cn.org.alan.exam.model.vo.answer.UncorrectedUserVO;

import cn.org.alan.exam.model.vo.answer.UserAnswerDetailVO;

import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;

import cn.org.alan.exam.service.IManualScoreService;

import cn.org.alan.exam.utils.ClassTokenGenerator;

import cn.org.alan.exam.utils.ExamGradingUtil;

import cn.org.alan.exam.utils.QuestionSubItemsUtil;

import cn.org.alan.exam.utils.SecurityUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import javax.annotation.Resource;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;

import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;





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

    @Resource

    private QuestionMapper questionMapper;

    @Resource

    private ExamQuestionMapper examQuestionMapper;

    @Resource

    private OptionMapper optionMapper;



    /**

     * 教师查看指定学员在某场考试中的主观题作答明细。

     * 以试卷题目为准（whetherMark=0 时），避免 AI 预评分写入 manual_score 后列表为空。

     */

    @Override

    public Result<List<UserAnswerDetailVO>> getDetail(Integer userId, Integer examId) {

        LambdaQueryWrapper<UserExamsScore> scoreQw = new LambdaQueryWrapper<>();

        scoreQw.eq(UserExamsScore::getUserId, userId)

                .eq(UserExamsScore::getExamId, examId)

                .eq(UserExamsScore::getState, 1)

                .last("limit 1");

        UserExamsScore userScore = userExamsScoreMapper.selectOne(scoreQw);

        if (userScore == null || !Integer.valueOf(0).equals(userScore.getWhetherMark())) {

            return Result.success("查询成功", Collections.emptyList());

        }

        if (!ExamGradingUtil.examPaperNeedsManualGrading(examId, examQuestionMapper, questionMapper)) {

            markObjectiveExamComplete(userId, examId);

            return Result.success("查询成功", Collections.emptyList());

        }



        List<UserAnswerDetailVO> list = new ArrayList<>();

        LambdaQueryWrapper<ExamQuestion> eqQw = new LambdaQueryWrapper<>();

        eqQw.eq(ExamQuestion::getExamId, examId).orderByAsc(ExamQuestion::getSort);

        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(eqQw);



        for (ExamQuestion examQuestion : examQuestions) {

            if (examQuestion.getQuestionId() == null) {

                continue;

            }

            Question question = questionMapper.selectById(examQuestion.getQuestionId());

            if (question == null) {

                continue;

            }

            ExamQuAnswer answer = findUserAnswer(userId, examId, question.getId());

            if (answer != null && hasTeacherManualScore(answer.getId())) {

                continue;

            }

            if (Integer.valueOf(5).equals(question.getQuType())) {

                if (!ExamGradingUtil.compoundNeedsManualGrading(question)) {

                    continue;

                }

                list.add(buildCompoundDetail(userId, examId, examQuestion, question, answer));

            } else if (Integer.valueOf(4).equals(question.getQuType())) {

                list.add(buildSaqDetail(userId, examId, examQuestion, question, answer));

            }

        }

        return Result.success("查询成功", list);

    }



    /**

     * 批量提交批改分数：插入 manual_score、更新 user_exams_score 总分与 whetherMark，并按及格线发证书。

     */

    @Override

    @Transactional

    public Result<String> correct(List<CorrectAnswerFrom> correctAnswerFroms) {

        if (correctAnswerFroms == null || correctAnswerFroms.isEmpty()) {

            return Result.failed("批改数据不能为空");

        }

        List<ManualScore> list = new ArrayList<>(correctAnswerFroms.size());

        AtomicInteger manualTotalScore = new AtomicInteger();

        Integer teacherId = SecurityUtil.getUserId();

        correctAnswerFroms.forEach(correctAnswerFrom -> {



            LambdaQueryWrapper<ExamQuAnswer> wrapper = new LambdaQueryWrapper<ExamQuAnswer>()

                    .select(ExamQuAnswer::getId)

                    .eq(ExamQuAnswer::getExamId, correctAnswerFrom.getExamId())

                    .eq(ExamQuAnswer::getUserId, correctAnswerFrom.getUserId())

                    .eq(ExamQuAnswer::getQuestionId, correctAnswerFrom.getQuestionId());



            ExamQuAnswer examQuAnswer = examQuAnswerMapper.selectOne(wrapper);

            if (examQuAnswer == null) {

                examQuAnswer = ensureAnswerRow(

                        correctAnswerFrom.getExamId(),

                        correctAnswerFrom.getUserId(),

                        correctAnswerFrom.getQuestionId());

            }



            removeScoresForAnswer(examQuAnswer.getId());



            ManualScore manualScore = new ManualScore();

            manualScore.setExamQuAnswerId(examQuAnswer.getId());

            manualScore.setScore(correctAnswerFrom.getScore());

            manualScore.setUserId(teacherId);

            list.add(manualScore);

            manualTotalScore.addAndGet(correctAnswerFrom.getScore() == null ? 0 : correctAnswerFrom.getScore());

        });

        manualScoreMapper.insertList(list);



        CorrectAnswerFrom correctAnswerFrom = correctAnswerFroms.get(0);

        LambdaUpdateWrapper<UserExamsScore> userExamsScoreLambdaUpdateWrapper = new LambdaUpdateWrapper<UserExamsScore>()

                .eq(UserExamsScore::getExamId, correctAnswerFrom.getExamId())

                .eq(UserExamsScore::getUserId, correctAnswerFrom.getUserId())

                .set(UserExamsScore::getWhetherMark, 1)

                .setSql("user_score = user_score + " + manualTotalScore.get());

        userExamsScoreMapper.update(userExamsScoreLambdaUpdateWrapper);



        LambdaQueryWrapper<Exam> examWrapper = new LambdaQueryWrapper<Exam>()

                .select(Exam::getId, Exam::getCertificateId, Exam::getPassedScore)

                .eq(Exam::getId, correctAnswerFrom.getExamId());

        Exam exam = examMapper.selectOne(examWrapper);

        if (exam.getCertificateId() != null && exam.getCertificateId() > 0) {

            LambdaQueryWrapper<UserExamsScore> examsScoreWrapper = new LambdaQueryWrapper<UserExamsScore>()

                    .select(UserExamsScore::getId, UserExamsScore::getUserScore)

                    .eq(UserExamsScore::getExamId, correctAnswerFrom.getExamId())

                    .eq(UserExamsScore::getUserId, correctAnswerFrom.getUserId());

            UserExamsScore userExamsScore = userExamsScoreMapper.selectOne(examsScoreWrapper);

            if (userExamsScore.getUserScore() >= exam.getPassedScore()) {

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

        List<AnswerExamVO> list = examMapper.selectMarkedList(page, SecurityUtil.getUserId(), SecurityUtil.getRole(), examName).getRecords();



        list.forEach(answerExamVO -> {

            answerExamVO.setClassSize(examGradeMapper.selectClassSize(answerExamVO.getExamId()));

            LambdaQueryWrapper<UserExamsScore> numberWrapper = new LambdaQueryWrapper<UserExamsScore>()

                    .eq(UserExamsScore::getExamId, answerExamVO.getExamId())

                    .eq(UserExamsScore::getState, 1);

            answerExamVO.setNumberOfApplicants(userExamsScoreMapper.selectCount(numberWrapper).intValue());

            LambdaQueryWrapper<UserExamsScore> correctedWrapper = new LambdaQueryWrapper<UserExamsScore>()

                    .in(UserExamsScore::getWhetherMark, -1, 1)

                    .eq(UserExamsScore::getExamId, answerExamVO.getExamId())

                    .eq(UserExamsScore::getState, 1);

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



    /** 阅卷提交时若缺作答行（如自动交卷未补录），补一条空记录以便写入人工分。 */
    private ExamQuAnswer ensureAnswerRow(Integer examId, Integer userId, Integer questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new ServiceRuntimeException("试题不存在，试题ID：" + questionId);
        }
        ExamQuAnswer row = new ExamQuAnswer();
        row.setExamId(examId);
        row.setUserId(userId);
        row.setQuestionId(questionId);
        row.setQuestionType(question.getQuType());
        if (Integer.valueOf(4).equals(question.getQuType()) || Integer.valueOf(5).equals(question.getQuType())) {
            row.setAnswerContent(Integer.valueOf(5).equals(question.getQuType()) ? "{}" : "");
        }
        row.setIsRight(Integer.valueOf(5).equals(question.getQuType()) ? -1 : 0);
        examQuAnswerMapper.insert(row);
        return row;
    }

    private ExamQuAnswer findUserAnswer(Integer userId, Integer examId, Integer questionId) {

        LambdaQueryWrapper<ExamQuAnswer> qw = new LambdaQueryWrapper<>();

        qw.eq(ExamQuAnswer::getUserId, userId)

                .eq(ExamQuAnswer::getExamId, examId)

                .eq(ExamQuAnswer::getQuestionId, questionId)

                .last("limit 1");

        return examQuAnswerMapper.selectOne(qw);

    }



    /** 是否已有教师确认过的人工分（AI 预填分 user_id 为空，不算已批改）。 */

    private boolean hasTeacherManualScore(Integer examQuAnswerId) {

        if (examQuAnswerId == null) {

            return false;

        }

        LambdaQueryWrapper<ManualScore> qw = new LambdaQueryWrapper<>();

        qw.eq(ManualScore::getExamQuAnswerId, examQuAnswerId)

                .isNotNull(ManualScore::getUserId);

        return manualScoreMapper.selectCount(qw) > 0;

    }



    private void removeScoresForAnswer(Integer examQuAnswerId) {

        LambdaQueryWrapper<ManualScore> qw = new LambdaQueryWrapper<>();

        qw.eq(ManualScore::getExamQuAnswerId, examQuAnswerId);

        manualScoreMapper.delete(qw);

    }



    /** 全客观卷误标为待阅时，恢复为无需阅卷，使成绩分析可见。 */
    private void markObjectiveExamComplete(Integer userId, Integer examId) {

        LambdaUpdateWrapper<UserExamsScore> uw = new LambdaUpdateWrapper<>();

        uw.eq(UserExamsScore::getUserId, userId)

                .eq(UserExamsScore::getExamId, examId)

                .eq(UserExamsScore::getState, 1)

                .eq(UserExamsScore::getWhetherMark, 0)

                .set(UserExamsScore::getWhetherMark, -1);

        userExamsScoreMapper.update(null, uw);

    }



    private UserAnswerDetailVO buildSaqDetail(Integer userId, Integer examId, ExamQuestion examQuestion,

                                              Question question, ExamQuAnswer answer) {

        UserAnswerDetailVO vo = new UserAnswerDetailVO();

        vo.setQuId(question.getId());

        vo.setUserId(userId);

        vo.setExamId(examId);

        vo.setQuType(4);

        vo.setQuTitle(question.getContent());

        vo.setQuImg(question.getImage());

        vo.setAnswer(answer != null && answer.getAnswerContent() != null ? answer.getAnswerContent() : "");

        vo.setRefAnswer(loadRefAnswer(question.getId()));

        vo.setCorrectScore(answer != null ? answer.getAiScore() : null);

        vo.setAiReason(answer != null ? answer.getAiReason() : null);

        vo.setTotalScore(examQuestion.getScore());

        return vo;

    }



    private String loadRefAnswer(Integer questionId) {

        LambdaQueryWrapper<Option> optQw = new LambdaQueryWrapper<>();

        optQw.eq(Option::getQuId, questionId)

                .eq(Option::getIsRight, 1)

                .orderByAsc(Option::getSort);

        List<Option> opts = optionMapper.selectList(optQw);

        if (opts == null || opts.isEmpty()) {

            return null;

        }

        return opts.stream()

                .map(Option::getContent)

                .filter(StringUtils::isNotBlank)

                .collect(Collectors.joining(" / "));

    }



    private UserAnswerDetailVO buildCompoundDetail(Integer userId, Integer examId, ExamQuestion examQuestion,

                                                   Question question, ExamQuAnswer answer) {

        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());

        List<QuestionSubItemVO> subItemList = QuestionSubItemsUtil.parseToVoList(question.getSubItems());

        String savedContent = answer != null ? answer.getAnswerContent() : "{}";

        QuestionSubItemsUtil.applyCompoundStudentAnswers(subItemList, savedContent);



        UserAnswerDetailVO vo = new UserAnswerDetailVO();

        vo.setQuId(question.getId());

        vo.setUserId(userId);

        vo.setExamId(examId);

        vo.setQuType(5);

        vo.setQuTitle(question.getContent());

        vo.setQuImg(question.getImage());

        vo.setSubItemList(subItemList);

        vo.setAnswer(formatCompoundStudentAnswer(subItems, savedContent));

        vo.setRefAnswer(formatCompoundReferenceAnswer(subItems));

        vo.setCorrectScore(answer != null ? answer.getAiScore() : null);

        vo.setAiReason(answer != null ? answer.getAiReason() : null);

        vo.setTotalScore(examQuestion.getScore());

        return vo;

    }



    private String formatCompoundStudentAnswer(List<QuestionSubItemForm> subItems, String answerJson) {

        Map<String, Object> answers = QuestionSubItemsUtil.parseStudentAnswers(answerJson);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < subItems.size(); i++) {

            QuestionSubItemForm sub = subItems.get(i);

            if (sub == null || !Integer.valueOf(4).equals(sub.getQuType())) {

                continue;

            }

            appendSubItemHeader(sb, i, sub.getContent());

            int slotCount = countSaqSlots(sub);

            List<String> slots = QuestionSubItemsUtil.parseSaqSlots(answers.get(String.valueOf(i)), slotCount);

            for (int j = 0; j < slots.size(); j++) {

                sb.append("  空").append(j + 1).append("：").append(slots.get(j)).append('\n');

            }

        }

        return sb.toString().trim();

    }



    private String formatCompoundReferenceAnswer(List<QuestionSubItemForm> subItems) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < subItems.size(); i++) {

            QuestionSubItemForm sub = subItems.get(i);

            if (sub == null || !Integer.valueOf(4).equals(sub.getQuType())) {

                continue;

            }

            appendSubItemHeader(sb, i, sub.getContent());

            List<QuestionSubItemOptionForm> opts = sub.getOptions();

            if (opts == null || opts.isEmpty()) {

                continue;

            }

            int slot = 0;

            for (QuestionSubItemOptionForm opt : opts) {

                if (opt == null || !Integer.valueOf(1).equals(opt.getIsRight())) {

                    continue;

                }

                sb.append("  空").append(++slot).append("：")

                        .append(opt.getContent() == null ? "" : opt.getContent()).append('\n');

            }

        }

        return sb.toString().trim();

    }



    private static void appendSubItemHeader(StringBuilder sb, int index, String content) {

        sb.append('(').append(index + 1).append(") ");

        if (StringUtils.isNotBlank(content)) {

            sb.append(content).append('\n');

        }

    }



    private static int countSaqSlots(QuestionSubItemForm sub) {

        if (sub.getOptions() == null || sub.getOptions().isEmpty()) {

            return 1;

        }

        int right = 0;

        for (QuestionSubItemOptionForm opt : sub.getOptions()) {

            if (opt != null && Integer.valueOf(1).equals(opt.getIsRight())) {

                right++;

            }

        }

        return right > 0 ? right : 1;

    }

}


