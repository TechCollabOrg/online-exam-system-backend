package cn.org.alan.exam.utils;

import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 判断试卷是否含需人工阅卷的题目（简答题或含简答子题的复合题）。
 */
public final class ExamGradingUtil {

    private ExamGradingUtil() {
    }

    /**
     * 按试卷实际题目判断是否需要教师阅卷，不依赖考试表上的 saq_count 配置。
     */
    public static boolean examPaperNeedsManualGrading(Integer examId,
                                                        ExamQuestionMapper examQuestionMapper,
                                                        QuestionMapper questionMapper) {
        if (examId == null || examQuestionMapper == null || questionMapper == null) {
            return false;
        }
        LambdaQueryWrapper<ExamQuestion> qw = new LambdaQueryWrapper<>();
        qw.eq(ExamQuestion::getExamId, examId);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(qw);
        for (ExamQuestion examQuestion : examQuestions) {
            if (examQuestion.getQuestionId() == null) {
                continue;
            }
            Question question = questionMapper.selectById(examQuestion.getQuestionId());
            if (questionNeedsManualGrading(question)) {
                return true;
            }
        }
        return false;
    }

    public static boolean questionNeedsManualGrading(Question question) {
        if (question == null) {
            return false;
        }
        if (Integer.valueOf(4).equals(question.getQuType())) {
            return true;
        }
        if (Integer.valueOf(5).equals(question.getQuType())) {
            return compoundNeedsManualGrading(question);
        }
        return false;
    }

    public static boolean compoundNeedsManualGrading(Question question) {
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        for (QuestionSubItemForm sub : subItems) {
            if (sub != null && Integer.valueOf(4).equals(sub.getQuType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 复合题机判：1=客观小题全对；0=有错或未答全；{-1}=含简答子题无法机判。
     */
    public static int gradeCompoundAnswer(Question question, String answerJson) {
        if (question == null || !Integer.valueOf(5).equals(question.getQuType())) {
            return 0;
        }
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        Map<String, Object> answers = QuestionSubItemsUtil.parseStudentAnswers(answerJson);
        if (subItems.isEmpty()) {
            return 0;
        }
        boolean hasManual = false;
        boolean allObjectiveCorrect = true;
        for (int i = 0; i < subItems.size(); i++) {
            QuestionSubItemForm sub = subItems.get(i);
            if (sub == null || sub.getQuType() == null) {
                allObjectiveCorrect = false;
                continue;
            }
            if (Integer.valueOf(4).equals(sub.getQuType())) {
                hasManual = true;
                continue;
            }
            if (!isObjectiveSubItemCorrect(sub, answers.get(String.valueOf(i)))) {
                allObjectiveCorrect = false;
            }
        }
        if (hasManual) {
            return -1;
        }
        return allObjectiveCorrect ? 1 : 0;
    }

    private static boolean isObjectiveSubItemCorrect(QuestionSubItemForm sub, Object ans) {
        List<QuestionSubItemOptionForm> opts = sub.getOptions();
        if (opts == null || opts.isEmpty()) {
            return false;
        }
        int quType = sub.getQuType();
        if (quType == 1 || quType == 3) {
            int selected = parseAnswerIndex(ans, -1);
            if (selected < 0 || selected >= opts.size()) {
                return false;
            }
            return Integer.valueOf(1).equals(opts.get(selected).getIsRight());
        }
        if (quType == 2) {
            Set<Integer> selected = parseAnswerIndexSet(ans);
            Set<Integer> correct = new LinkedHashSet<>();
            for (int i = 0; i < opts.size(); i++) {
                if (Integer.valueOf(1).equals(opts.get(i).getIsRight())) {
                    correct.add(i);
                }
            }
            return !selected.isEmpty() && selected.equals(correct);
        }
        return false;
    }

    private static int parseAnswerIndex(Object ans, int defaultVal) {
        if (ans == null) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(String.valueOf(ans).trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static Set<Integer> parseAnswerIndexSet(Object ans) {
        Set<Integer> out = new LinkedHashSet<>();
        if (ans == null) {
            return out;
        }
        if (ans instanceof JSONArray) {
            JSONArray arr = (JSONArray) ans;
            for (int i = 0; i < arr.size(); i++) {
                out.add(arr.getInteger(i));
            }
            return out;
        }
        String s = String.valueOf(ans).trim();
        if (s.startsWith("[")) {
            try {
                JSONArray arr = JSONArray.parseArray(s);
                for (int i = 0; i < arr.size(); i++) {
                    out.add(arr.getInteger(i));
                }
                return out;
            } catch (Exception ignored) {
                return out;
            }
        }
        for (String part : s.split(",")) {
            try {
                out.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return out;
    }
}
