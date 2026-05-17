package cn.org.alan.exam.service.impl;

import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.ExamMapper;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.mapper.GradeMapper;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.mapper.UserExamsScoreMapper;
import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Grade;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.vo.score.ScoreAiBriefingVO;
import cn.org.alan.exam.model.vo.score.ScoreBriefingRowVO;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;
import cn.org.alan.exam.service.IScoreAiBriefingService;
import cn.org.alan.exam.utils.ScoreBriefingStatsUtil;
import cn.org.alan.exam.utils.agent.AIChat;
import cn.org.alan.exam.utils.agent.Constants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聚合班级考试成绩数据并调用大模型生成自然语言简报。
 */
@Slf4j
@Service
public class ScoreAiBriefingServiceImpl implements IScoreAiBriefingService {

    private static final int MAX_WEAK_QUESTIONS = 5;
    private static final int MAX_QUESTIONS_SCAN = 25;

    @Resource
    private ExamMapper examMapper;
    @Resource
    private GradeMapper gradeMapper;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private ExamQuestionMapper examQuestionMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;
    @Resource
    private AIChat aiChat;

    @Override
    public Result<ScoreAiBriefingVO> generateBriefing(Integer examId, Integer gradeId) {
        if (examId == null || gradeId == null) {
            return Result.failed("考试与班级不能为空");
        }
        Exam exam = examMapper.selectById(examId);
        if (exam == null || Integer.valueOf(1).equals(exam.getIsDeleted())) {
            return Result.failed("考试不存在");
        }
        Grade grade = gradeMapper.selectById(gradeId);
        if (grade == null) {
            return Result.failed("班级不存在");
        }

        List<ScoreBriefingRowVO> rows = userExamsScoreMapper.listBriefingScores(examId, gradeId);
        if (rows == null || rows.isEmpty()) {
            return Result.failed("暂无已出分的参考成绩，无法生成简报");
        }

        int fullScore = exam.getGrossScore() != null && exam.getGrossScore() > 0 ? exam.getGrossScore() : 100;
        int passScore = ScoreBriefingStatsUtil.resolvePassScore(exam.getPassedScore(), fullScore);
        boolean passDefault = exam.getPassedScore() == null || exam.getPassedScore() <= 0;

        List<Integer> scoreList = rows.stream()
                .map(ScoreBriefingRowVO::getUserScore)
                .filter(s -> s != null)
                .collect(Collectors.toList());

        int attend = scoreList.size();
        int max = scoreList.stream().max(Integer::compareTo).orElse(0);
        int min = scoreList.stream().min(Integer::compareTo).orElse(0);
        double avg = scoreList.stream().mapToInt(Integer::intValue).average().orElse(0);
        long passCount = scoreList.stream().filter(s -> s >= passScore).count();
        double passRate = attend > 0 ? Math.round(passCount * 1000.0 / attend) / 10.0 : 0;

        double avgCut = rows.stream()
                .filter(r -> r.getCutScreenCount() != null)
                .mapToInt(ScoreBriefingRowVO::getCutScreenCount)
                .average()
                .orElse(0);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("考试名称", exam.getTitle());
        context.put("班级名称", grade.getGradeName());
        context.put("满分", fullScore);
        context.put("及格分", passScore);
        context.put("及格分说明", passDefault ? "试卷未设及格分时按满分60%计算" : "试卷设置的及格分");
        context.put("参考人数", attend);
        context.put("最高分", max);
        context.put("最低分", min);
        context.put("平均分", Math.round(avg * 10) / 10.0);
        context.put("及格人数", passCount);
        context.put("及格率", passRate + "%");
        context.put("平均切屏次数", Math.round(avgCut * 10) / 10.0);
        context.put("成绩分档", ScoreBriefingStatsUtil.buildGradeBuckets(rows, fullScore, passScore));
        context.put("低正确率题目", buildWeakQuestions(examId));

        String userPayload = JSONUtil.toJsonPrettyStr(context);
        try {
            String briefing = aiChat.getChatResponse(Constants.scoreBriefingSystemMessage, userPayload);
            if (StringUtils.isBlank(briefing)) {
                return Result.failed("AI 未返回有效内容，请检查 LLM 配置");
            }
            ScoreAiBriefingVO vo = new ScoreAiBriefingVO();
            vo.setBriefing(briefing.trim());
            vo.setExamTitle(exam.getTitle());
            vo.setGradeName(grade.getGradeName());
            vo.setAttendCount(attend);
            return Result.success("简报生成成功", vo);
        } catch (Exception e) {
            log.error("成绩 AI 简报失败 examId={} gradeId={}", examId, gradeId, e);
            return Result.failed("AI 调用失败：" + e.getMessage());
        }
    }

    private List<Map<String, Object>> buildWeakQuestions(Integer examId) {
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSort)
                        .last("LIMIT " + MAX_QUESTIONS_SCAN));
        if (examQuestions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> analysed = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            if (eq.getQuestionId() == null) {
                continue;
            }
            QuestionAnalyseVO stat = examQuAnswerMapper.questionAnalyse(examId, eq.getQuestionId());
            if (stat == null || stat.getTotalCount() == null || stat.getTotalCount() <= 0) {
                continue;
            }
            int right = stat.getRightCount() != null ? stat.getRightCount() : 0;
            double accuracy = Math.round(right * 1000.0 / stat.getTotalCount()) / 10.0;

            Question q = questionMapper.selectById(eq.getQuestionId());
            String stem = q != null ? stripHtml(q.getContent()) : ("题目ID " + eq.getQuestionId());
            if (stem.length() > 120) {
                stem = stem.substring(0, 120) + "…";
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("题目ID", eq.getQuestionId());
            item.put("题干摘要", stem);
            item.put("作答人数", stat.getTotalCount());
            item.put("正确人数", right);
            item.put("正确率", accuracy + "%");
            item.put("题型", typeLabel(eq.getType()));
            analysed.add(item);
        }
        analysed.sort(Comparator.comparingDouble(m -> parseAccuracyPercent(m.get("正确率"))));
        return analysed.stream().limit(MAX_WEAK_QUESTIONS).collect(Collectors.toList());
    }

    private static double parseAccuracyPercent(Object rateObj) {
        if (rateObj == null) {
            return 100;
        }
        String s = String.valueOf(rateObj).replace("%", "").trim();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    private static String typeLabel(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case 1:
                return "单选";
            case 2:
                return "多选";
            case 3:
                return "判断";
            case 4:
                return "简答";
            case 5:
                return "复合";
            default:
                return "题型" + type;
        }
    }

    private static String stripHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
