package cn.org.alan.exam.service.impl;

import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.form.ai.AiQuestionReviewForm;
import cn.org.alan.exam.model.vo.ai.AiQuestionReviewVO;
import cn.org.alan.exam.model.vo.exam.OptionVO;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import cn.org.alan.exam.model.vo.record.ExamRecordDetailVO;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import cn.org.alan.exam.service.IAiQuestionReviewService;
import cn.org.alan.exam.service.IExerciseRecordService;
import cn.org.alan.exam.utils.AiGradingTextUtil;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.agent.AIChatRouter;
import cn.org.alan.exam.utils.agent.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据考后答卷数据生成单题 AI 解析。
 */
@Service
public class AiQuestionReviewServiceImpl implements IAiQuestionReviewService {

    @Resource
    private IExerciseRecordService exerciseRecordService;
    @Resource
    private AIChatRouter aiChatRouter;
    @Resource
    private IAiPlatformConfigService aiPlatformConfigService;

    @Override
    public Result<AiQuestionReviewVO> analyzeQuestion(AiQuestionReviewForm form) {
        if (!isAiConfigured()) {
            return Result.failed("请由管理员在「API 连接配置」中保存并启用 AI 接口后再使用");
        }
        Integer targetUserId = resolveTargetUserId(form.getUserId());
        Result<List<ExamRecordDetailVO>> detailRes =
                exerciseRecordService.getExamRecordDetail(form.getExamId(), targetUserId);
        if (detailRes == null || detailRes.getData() == null) {
            return Result.failed("未找到答卷数据");
        }
        ExamRecordDetailVO question = detailRes.getData().stream()
                .filter(q -> form.getQuId().equals(q.getQuId()))
                .findFirst()
                .orElse(null);
        if (question == null) {
            return Result.failed("未找到该题目作答记录");
        }
        try {
            String payload = JSONUtil.toJsonPrettyStr(buildQuestionContext(question, form.getSubIndex()));
            String text = aiChatRouter.getChatResponse(Constants.studentQuestionReviewSystemMessage, payload);
            if (StringUtils.isBlank(text)) {
                return Result.failed("AI 未返回有效内容");
            }
            AiQuestionReviewVO vo = new AiQuestionReviewVO();
            vo.setAnalysis(text.trim());
            return Result.success("解析完成", vo);
        } catch (Exception e) {
            return Result.failed("AI 解析失败：" + e.getMessage());
        }
    }

    private boolean isAiConfigured() {
        LlmResolvedConfig active = aiPlatformConfigService.resolveActive();
        return active != null && StringUtils.isNotBlank(active.getApiKey());
    }

    private Integer resolveTargetUserId(Integer requestedUserId) {
        Integer loginId = SecurityUtil.getUserId();
        if (requestedUserId == null) {
            return loginId;
        }
        Integer roleCode = SecurityUtil.getRoleCode();
        if (Integer.valueOf(1).equals(roleCode) && !loginId.equals(requestedUserId)) {
            throw new ServiceRuntimeException("无权查看其他考生的答卷解析");
        }
        return requestedUserId;
    }

    private Map<String, Object> buildQuestionContext(ExamRecordDetailVO q, Integer subIndex) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("题型", typeLabel(q.getQuType()));
        ctx.put("题干", AiGradingTextUtil.stripHtml(q.getTitle()));
        ctx.put("是否答对", rightLabel(q.getIsRight()));

        if (Integer.valueOf(5).equals(q.getQuType()) && q.getSubItemList() != null && !q.getSubItemList().isEmpty()) {
            if (subIndex != null && subIndex >= 0 && subIndex < q.getSubItemList().size()) {
                ctx.put("分析范围", "复合题第 " + (subIndex + 1) + " 小题");
                ctx.put("子题", buildSubItemMap(q.getSubItemList().get(subIndex), subIndex));
            } else {
                List<Map<String, Object>> subs = new ArrayList<>();
                for (int i = 0; i < q.getSubItemList().size(); i++) {
                    subs.add(buildSubItemMap(q.getSubItemList().get(i), i));
                }
                ctx.put("子题列表", subs);
            }
        } else {
            ctx.put("考生答案", formatStudentAnswer(q));
            ctx.put("正确答案", formatRightAnswer(q));
            if (q.getOption() != null && !q.getOption().isEmpty()) {
                ctx.put("选项", formatOptions(q.getOption()));
            }
        }

        String analyse = AiGradingTextUtil.stripHtml(q.getAnalyse());
        if (StringUtils.isNotBlank(analyse)) {
            ctx.put("题库解析", analyse);
        }
        return ctx;
    }

    private Map<String, Object> buildSubItemMap(QuestionSubItemVO sub, int index) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("序号", index + 1);
        m.put("子题型", typeLabel(sub.getQuType()));
        m.put("题干", AiGradingTextUtil.stripHtml(sub.getContent()));
        m.put("考生答案", formatSubStudentAnswer(sub));
        m.put("正确答案", formatSubRightAnswer(sub));
        return m;
    }

    private static String typeLabel(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case 1:
                return "单选题";
            case 2:
                return "多选题";
            case 3:
                return "判断题";
            case 4:
                return "简答题";
            case 5:
                return "复合题";
            default:
                return "题型" + type;
        }
    }

    private static String rightLabel(Integer isRight) {
        if (isRight == null || isRight == -1) {
            return "未判分/待阅";
        }
        return isRight == 1 ? "答对" : "答错";
    }

    private static String formatStudentAnswer(ExamRecordDetailVO q) {
        if (q.getMyOption() == null || q.getMyOption().trim().isEmpty()) {
            return "未作答";
        }
        if (Integer.valueOf(4).equals(q.getQuType())) {
            return AiGradingTextUtil.stripHtml(q.getMyOption());
        }
        return indexToLetters(q.getMyOption());
    }

    private static String formatRightAnswer(ExamRecordDetailVO q) {
        if (Integer.valueOf(4).equals(q.getQuType())) {
            if (q.getOption() != null && !q.getOption().isEmpty()) {
                return AiGradingTextUtil.stripHtml(q.getOption().get(0).getContent());
            }
            return q.getRightOption() != null ? q.getRightOption() : "—";
        }
        return indexToLetters(q.getRightOption());
    }

    private static List<Map<String, String>> formatOptions(List<Option> options) {
        List<Map<String, String>> list = new ArrayList<>();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            if (opt == null) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            String label = i < letters.length() ? String.valueOf(letters.charAt(i)) : String.valueOf(i);
            row.put("选项", label);
            row.put("内容", AiGradingTextUtil.stripHtml(opt.getContent()));
            row.put("是否正确", (opt.getIsRight() != null && opt.getIsRight() == 1) ? "是" : "否");
            list.add(row);
        }
        return list;
    }

    private static String formatSubStudentAnswer(QuestionSubItemVO sub) {
        if (sub == null) {
            return "未作答";
        }
        if (Integer.valueOf(4).equals(sub.getQuType())) {
            String raw = sub.getStudentFill() != null ? sub.getStudentFill() : sub.getStudentAnswer();
            if (raw == null || raw.trim().isEmpty()) {
                return "未作答";
            }
            return AiGradingTextUtil.stripHtml(raw);
        }
        if (sub.getStudentAnswer() == null || sub.getStudentAnswer().trim().isEmpty()) {
            return "未作答";
        }
        return indexToLetters(sub.getStudentAnswer());
    }

    private static String formatSubRightAnswer(QuestionSubItemVO sub) {
        if (sub == null || sub.getOptions() == null || sub.getOptions().isEmpty()) {
            return "—";
        }
        if (Integer.valueOf(4).equals(sub.getQuType())) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sub.getOptions().size(); i++) {
                if (i > 0) {
                    sb.append("；");
                }
                sb.append("空").append(i + 1).append("：")
                        .append(AiGradingTextUtil.stripHtml(sub.getOptions().get(i).getContent()));
            }
            return sb.toString();
        }
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        List<String> rights = new ArrayList<>();
        for (int i = 0; i < sub.getOptions().size(); i++) {
            OptionVO opt = sub.getOptions().get(i);
            if (opt != null && opt.getIsRight() != null && opt.getIsRight() == 1) {
                rights.add(i < letters.length() ? String.valueOf(letters.charAt(i)) : String.valueOf(i));
            }
        }
        return rights.isEmpty() ? "—" : String.join("、", rights);
    }

    private static String indexToLetters(String indices) {
        if (indices == null || indices.trim().isEmpty()) {
            return "—";
        }
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String[] parts = indices.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append("、");
            }
            try {
                int idx = Integer.parseInt(parts[i].trim());
                sb.append(idx >= 0 && idx < letters.length() ? letters.charAt(idx) : parts[i]);
            } catch (NumberFormatException e) {
                sb.append(parts[i]);
            }
        }
        return sb.toString();
    }
}
