package cn.org.alan.exam.utils;

import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.mapper.OptionMapper;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm;
import cn.org.alan.exam.model.vo.question.QuestionScoreVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按试卷题目顺序加载待 AI 阅卷题目（与教师 {@code getDetail} 一致：简答题 + 含简答子题的复合题）。
 */
@Component
public class AiGradingQuestionLoader {

    @Resource
    private ExamQuestionMapper examQuestionMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;
    @Resource
    private OptionMapper optionMapper;

    public List<QuestionScoreVO> load(Integer examId, Integer userId) {
        LambdaQueryWrapper<ExamQuestion> eqQw = new LambdaQueryWrapper<>();
        eqQw.eq(ExamQuestion::getExamId, examId).orderByAsc(ExamQuestion::getSort);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(eqQw);
        List<QuestionScoreVO> list = new ArrayList<>();
        for (ExamQuestion examQuestion : examQuestions) {
            if (examQuestion.getQuestionId() == null) {
                continue;
            }
            Question question = questionMapper.selectById(examQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            if (Integer.valueOf(4).equals(question.getQuType())) {
                list.add(buildSaq(examId, userId, examQuestion, question));
            } else if (Integer.valueOf(5).equals(question.getQuType())
                    && ExamGradingUtil.compoundNeedsManualGrading(question)) {
                list.add(buildCompound(examId, userId, examQuestion, question));
            }
        }
        return list;
    }

    private QuestionScoreVO buildSaq(Integer examId, Integer userId, ExamQuestion examQuestion, Question question) {
        ExamQuAnswer answer = findOrCreateAnswer(examId, userId, question);
        QuestionScoreVO vo = new QuestionScoreVO();
        vo.setQuestionId(String.valueOf(question.getId()));
        vo.setQuestionContent(question.getContent());
        vo.setTotalScore(examQuestion.getScore());
        vo.setQusetionAnswer(loadRefAnswer(question.getId()));
        vo.setQuestionAnalysis(question.getAnalysis());
        vo.setUserAnswer(answer != null && answer.getAnswerContent() != null ? answer.getAnswerContent() : "");
        vo.setReferenceMaterial("");
        return vo;
    }

    private QuestionScoreVO buildCompound(Integer examId, Integer userId, ExamQuestion examQuestion, Question question) {
        ExamQuAnswer answer = findOrCreateAnswer(examId, userId, question);
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        String savedContent = answer != null && answer.getAnswerContent() != null ? answer.getAnswerContent() : "{}";
        QuestionScoreVO vo = new QuestionScoreVO();
        vo.setQuestionId(String.valueOf(question.getId()));
        vo.setQuestionContent(question.getContent());
        vo.setTotalScore(examQuestion.getScore());
        vo.setQusetionAnswer(formatCompoundReferenceAnswer(subItems));
        vo.setQuestionAnalysis(question.getAnalysis());
        vo.setUserAnswer(formatCompoundStudentAnswer(subItems, savedContent));
        vo.setReferenceMaterial("");
        return vo;
    }

    private ExamQuAnswer findOrCreateAnswer(Integer examId, Integer userId, Question question) {
        ExamQuAnswer existing = findUserAnswer(userId, examId, question.getId());
        if (existing != null) {
            return existing;
        }
        ExamQuAnswer row = new ExamQuAnswer();
        row.setExamId(examId);
        row.setUserId(userId);
        row.setQuestionId(question.getId());
        row.setQuestionType(question.getQuType());
        if (Integer.valueOf(5).equals(question.getQuType())) {
            row.setAnswerContent("{}");
            row.setIsRight(-1);
        } else {
            row.setAnswerContent("");
            row.setIsRight(0);
        }
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

    private String loadRefAnswer(Integer questionId) {
        LambdaQueryWrapper<Option> optQw = new LambdaQueryWrapper<>();
        optQw.eq(Option::getQuId, questionId)
                .eq(Option::getIsRight, 1)
                .orderByAsc(Option::getSort);
        List<Option> opts = optionMapper.selectList(optQw);
        if (opts == null || opts.isEmpty()) {
            return "";
        }
        return opts.stream()
                .map(Option::getContent)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" / "));
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

    private static int countSaqSlots(QuestionSubItemForm sub) {
        if (sub.getOptions() == null || sub.getOptions().isEmpty()) {
            return 1;
        }
        long rights = sub.getOptions().stream()
                .filter(o -> o != null && Integer.valueOf(1).equals(o.getIsRight()))
                .count();
        return (int) Math.max(1, rights);
    }

    private static void appendSubItemHeader(StringBuilder sb, int index, String content) {
        sb.append('(').append(index + 1).append(") ");
        if (StringUtils.isNotBlank(content)) {
            sb.append(AiGradingTextUtil.stripHtml(content)).append('\n');
        }
    }
}
