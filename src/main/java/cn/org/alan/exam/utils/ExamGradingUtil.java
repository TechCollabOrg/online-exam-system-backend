package cn.org.alan.exam.utils;

import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 阅卷流程辅助：判断试卷/复合题是否含需人工或 AI 辅助评分的主观小题。
 */
public final class ExamGradingUtil {

    private ExamGradingUtil() {
    }

    /**
     * 复合题机判：纯客观小题返回 1/0；含简答子题返回 -1（待人工/AI 阅卷）。
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
            if (sub.getQuType() == 4) {
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

    /**
     * 复合题是否含简答子题（需人工/AI 阅卷）。
     */
    public static boolean compoundNeedsManualGrading(Question question) {
        if (question == null || !Integer.valueOf(5).equals(question.getQuType())) {
            return false;
        }
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        for (QuestionSubItemForm sub : subItems) {
            if (sub != null && Integer.valueOf(4).equals(sub.getQuType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 试卷是否含简答题或含简答小问的复合题。
     */
    public static boolean examPaperNeedsManualGrading(Integer examId,
                                                      ExamQuestionMapper examQuestionMapper,
                                                      QuestionMapper questionMapper) {
        if (examId == null || examQuestionMapper == null || questionMapper == null) {
            return false;
        }
        LambdaQueryWrapper<ExamQuestion> eqQw = new LambdaQueryWrapper<>();
        eqQw.eq(ExamQuestion::getExamId, examId);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(eqQw);
        if (examQuestions == null || examQuestions.isEmpty()) {
            return false;
        }
        boolean hasSaqType = examQuestions.stream()
                .anyMatch(eq -> Integer.valueOf(4).equals(eq.getType()));
        if (hasSaqType) {
            return true;
        }
        List<Integer> compoundQuIds = examQuestions.stream()
                .filter(eq -> Integer.valueOf(5).equals(eq.getType()))
                .map(ExamQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (compoundQuIds.isEmpty()) {
            return false;
        }
        List<Question> compounds = questionMapper.selectBatchIds(compoundQuIds);
        if (compounds == null) {
            return false;
        }
        for (Question q : compounds) {
            if (compoundNeedsManualGrading(q)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 复合题客观小题数量与答对数量（用于含简答复合题交卷时部分给分）。
     */
    public static int[] countCompoundObjectiveResult(Question question, String answerJson) {
        if (question == null || !Integer.valueOf(5).equals(question.getQuType())) {
            return new int[]{0, 0};
        }
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        if (subItems.isEmpty()) {
            return new int[]{0, 0};
        }
        Map<String, Object> answers = QuestionSubItemsUtil.parseStudentAnswers(answerJson);
        int total = 0;
        int correct = 0;
        for (int i = 0; i < subItems.size(); i++) {
            QuestionSubItemForm sub = subItems.get(i);
            if (sub == null || sub.getQuType() == null || sub.getQuType() == 4) {
                continue;
            }
            total++;
            if (isObjectiveSubItemCorrect(sub, answers.get(String.valueOf(i)))) {
                correct++;
            }
        }
        return new int[]{total, correct};
    }

    /**
     * 复合题中单道客观小题是否答对（下标与前端 {@code CompoundQuestionDisplay} 一致）。
     */
    public static boolean isObjectiveSubItemCorrect(QuestionSubItemForm sub, Object ans) {
        List<cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm> opts = sub.getOptions();
        if (opts == null || opts.isEmpty()) {
            return false;
        }
        int quType = sub.getQuType();
        if (quType == 1 || quType == 3) {
            int selected = parseAnswerIndex(ans, -1);
            if (selected < 0 || selected >= opts.size()) {
                return false;
            }
            return QuestionSubItemsUtil.isOptionCorrect(opts.get(selected).getIsRight());
        }
        if (quType == 2) {
            java.util.Set<Integer> selected = parseAnswerIndexSet(ans);
            java.util.Set<Integer> correct = new java.util.LinkedHashSet<>();
            for (int i = 0; i < opts.size(); i++) {
                if (QuestionSubItemsUtil.isOptionCorrect(opts.get(i).getIsRight())) {
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

    private static java.util.Set<Integer> parseAnswerIndexSet(Object ans) {
        java.util.Set<Integer> out = new java.util.LinkedHashSet<>();
        if (ans == null) {
            return out;
        }
        if (ans instanceof com.alibaba.fastjson.JSONArray) {
            com.alibaba.fastjson.JSONArray arr = (com.alibaba.fastjson.JSONArray) ans;
            for (int i = 0; i < arr.size(); i++) {
                int idx = parseAnswerIndex(arr.get(i), -1);
                if (idx >= 0) {
                    out.add(idx);
                }
            }
            return out;
        }
        String s = String.valueOf(ans).trim();
        if (s.startsWith("[")) {
            try {
                com.alibaba.fastjson.JSONArray arr = com.alibaba.fastjson.JSON.parseArray(s);
                for (int i = 0; i < arr.size(); i++) {
                    int idx = parseAnswerIndex(arr.get(i), -1);
                    if (idx >= 0) {
                        out.add(idx);
                    }
                }
                return out;
            } catch (Exception ignored) {
                return out;
            }
        }
        if (s.contains(",")) {
            for (String part : s.split(",")) {
                int idx = parseAnswerIndex(part, -1);
                if (idx >= 0) {
                    out.add(idx);
                }
            }
            return out;
        }
        int single = parseAnswerIndex(s, -1);
        if (single >= 0) {
            out.add(single);
        }
        return out;
    }

    /**
     * 构建供 AI 阅卷的复合题标准答案与考生答案文本（仅简答子题）。
     */
    public static String formatCompoundSaqReference(Question question) {
        if (question == null) {
            return "";
        }
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subItems.size(); i++) {
            QuestionSubItemForm sub = subItems.get(i);
            if (sub == null || !Integer.valueOf(4).equals(sub.getQuType())) {
                continue;
            }
            appendSubHeader(sb, i, sub.getContent());
            List<cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm> opts = sub.getOptions();
            if (opts == null) {
                continue;
            }
            for (int j = 0; j < opts.size(); j++) {
                cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm opt = opts.get(j);
                if (opt != null && opt.getContent() != null) {
                    sb.append("  空").append(j + 1).append("：").append(stripSimpleHtml(opt.getContent())).append('\n');
                }
            }
        }
        return sb.toString().trim();
    }

    public static String formatCompoundSaqStudentAnswer(Question question, String answerJson) {
        if (question == null) {
            return "";
        }
        List<QuestionSubItemForm> subItems = QuestionSubItemsUtil.parseForms(question.getSubItems());
        Map<String, Object> answers = QuestionSubItemsUtil.parseStudentAnswers(answerJson);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subItems.size(); i++) {
            QuestionSubItemForm sub = subItems.get(i);
            if (sub == null || !Integer.valueOf(4).equals(sub.getQuType())) {
                continue;
            }
            appendSubHeader(sb, i, sub.getContent());
            int slotCount = sub.getOptions() == null ? 1 : Math.max(1, sub.getOptions().size());
            List<String> slots = QuestionSubItemsUtil.parseSaqSlots(answers.get(String.valueOf(i)), slotCount);
            for (int j = 0; j < slots.size(); j++) {
                sb.append("  空").append(j + 1).append("：").append(slots.get(j)).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private static void appendSubHeader(StringBuilder sb, int index, String content) {
        sb.append('(').append(index + 1).append(") ");
        if (content != null && !content.trim().isEmpty()) {
            sb.append(stripSimpleHtml(content)).append('\n');
        }
    }

    private static String stripSimpleHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
