package cn.org.alan.exam.service.impl;

import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.entity.Repo;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgeTreeNodeVO;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgeTreeVO;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgePointOptionVO;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import cn.org.alan.exam.service.IRepoKnowledgeTreeService;
import cn.org.alan.exam.service.IRepoService;
import cn.org.alan.exam.utils.AiGradingTextUtil;
import cn.org.alan.exam.utils.RepoKnowledgeTreeParser;
import cn.org.alan.exam.utils.RepoKnowledgeTreeUtil;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.agent.AIChatRouter;
import cn.org.alan.exam.utils.agent.Constants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据题库内题目调用 AI 生成知识点树并持久化到题库记录。
 */
@Service
public class RepoKnowledgeTreeServiceImpl implements IRepoKnowledgeTreeService {

    private static final int MAX_QUESTIONS_FOR_AI = 80;
    private static final int MAX_CONTENT_CHARS = 200;
    private static final int MAX_ANALYSIS_CHARS = 120;

    @Resource
    private IRepoService repoService;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private AIChatRouter aiChatRouter;
    @Resource
    private IAiPlatformConfigService aiPlatformConfigService;

    @Override
    public Result<RepoKnowledgeTreeVO> getKnowledgeTree(Integer repoId) {
        Repo repo = assertRepoAccessible(repoId);
        RepoKnowledgeTreeVO vo = buildBaseVo(repo);
        if (StringUtils.isBlank(repo.getKnowledgeTree())) {
            vo.setGenerated(false);
            return Result.success("暂无知识树，可点击生成", vo);
        }
        RepoKnowledgeTreeVO stored = JSONUtil.toBean(repo.getKnowledgeTree(), RepoKnowledgeTreeVO.class);
        if (stored != null) {
            vo.setRootName(stored.getRootName());
            vo.setNodes(stored.getNodes());
            vo.setTotalQuestions(stored.getTotalQuestions());
            vo.setGeneratedAt(stored.getGeneratedAt());
        }
        vo.setGenerated(true);
        return Result.success("获取知识树成功", vo);
    }

    @Override
    public Result<RepoKnowledgeTreeVO> generateKnowledgeTree(Integer repoId) {
        if (!isAiConfigured()) {
            return Result.failed("请由管理员在「API 连接配置」中保存并启用 AI 接口后再使用");
        }
        Repo repo = assertRepoAccessible(repoId);
        List<Question> questions = listQuestions(repoId);
        if (questions.isEmpty()) {
            return Result.failed("题库中没有题目，无法生成知识树");
        }

        try {
            String payload = buildQuestionPayload(repo, questions);
            String response = aiChatRouter.getGradingResponse(Constants.repoKnowledgeTreeSystemMessage, payload);
            RepoKnowledgeTreeVO parsed = RepoKnowledgeTreeParser.parse(response);
            if (parsed == null || parsed.getNodes() == null || parsed.getNodes().isEmpty()) {
                return Result.failed("AI 未返回有效的知识树结构，请稍后重试");
            }

            LocalDateTime now = LocalDateTime.now();
            parsed.setRepoId(repoId);
            parsed.setRepoTitle(repo.getTitle());
            parsed.setTotalQuestions(questions.size());
            parsed.setGeneratedAt(now);
            parsed.setGenerated(true);

            Repo update = new Repo();
            update.setId(repoId);
            update.setKnowledgeTree(JSONUtil.toJsonStr(parsed));
            update.setKnowledgeTreeTime(now);
            repoService.updateById(update);

            return Result.success("知识树生成成功", parsed);
        } catch (Exception e) {
            return Result.failed("知识树生成失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<RepoKnowledgePointOptionVO>> listKnowledgePointOptions(Integer repoId) {
        Repo repo = assertRepoAccessible(repoId);
        if (StringUtils.isBlank(repo.getKnowledgeTree())) {
            return Result.success("暂无知识树", new ArrayList<>());
        }
        RepoKnowledgeTreeVO stored = JSONUtil.toBean(repo.getKnowledgeTree(), RepoKnowledgeTreeVO.class);
        List<RepoKnowledgeTreeNodeVO> nodes = stored != null ? stored.getNodes() : null;
        return Result.success("获取知识点选项成功", RepoKnowledgeTreeUtil.flattenOptions(nodes));
    }

    @Override
    public List<Integer> resolveQuestionIds(Integer repoId, String knowledgePointPath) {
        if (repoId == null || StringUtils.isBlank(knowledgePointPath)) {
            return new ArrayList<>();
        }
        Repo repo = repoService.getById(repoId);
        if (repo == null || StringUtils.isBlank(repo.getKnowledgeTree())) {
            return new ArrayList<>();
        }
        RepoKnowledgeTreeVO stored = JSONUtil.toBean(repo.getKnowledgeTree(), RepoKnowledgeTreeVO.class);
        if (stored == null || stored.getNodes() == null) {
            return new ArrayList<>();
        }
        return RepoKnowledgeTreeUtil.resolveQuestionIds(stored.getNodes(), knowledgePointPath.trim());
    }

    private Repo assertRepoAccessible(Integer repoId) {
        Repo repo = repoService.getById(repoId);
        if (repo == null) {
            throw new ServiceRuntimeException("题库不存在");
        }
        Integer roleCode = SecurityUtil.getRoleCode();
        if (Integer.valueOf(2).equals(roleCode) && !SecurityUtil.getUserId().equals(repo.getUserId())) {
            throw new ServiceRuntimeException("无权操作该题库");
        }
        return repo;
    }

    private List<Question> listQuestions(Integer repoId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getRepoId, repoId)
                .orderByAsc(Question::getId)
                .last("LIMIT " + MAX_QUESTIONS_FOR_AI);
        List<Question> list = questionMapper.selectList(wrapper);
        return list != null ? list : new ArrayList<>();
    }

    private RepoKnowledgeTreeVO buildBaseVo(Repo repo) {
        RepoKnowledgeTreeVO vo = new RepoKnowledgeTreeVO();
        vo.setRepoId(repo.getId());
        vo.setRepoTitle(repo.getTitle());
        vo.setGeneratedAt(repo.getKnowledgeTreeTime());
        return vo;
    }

    private boolean isAiConfigured() {
        LlmResolvedConfig active = aiPlatformConfigService.resolveActive();
        return active != null && StringUtils.isNotBlank(active.getApiKey());
    }

    private String buildQuestionPayload(Repo repo, List<Question> questions) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("题库名称", repo.getTitle());
        root.put("题目总数", questions.size());
        if (questions.size() >= MAX_QUESTIONS_FOR_AI) {
            root.put("说明", "题目较多，仅分析前 " + MAX_QUESTIONS_FOR_AI + " 道，请据此归纳知识结构");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("题目ID", q.getId());
            item.put("题型", typeLabel(q.getQuType()));
            item.put("题干", truncate(AiGradingTextUtil.stripHtml(q.getContent()), MAX_CONTENT_CHARS));
            String analysis = truncate(AiGradingTextUtil.stripHtml(q.getAnalysis()), MAX_ANALYSIS_CHARS);
            if (StringUtils.isNotBlank(analysis)) {
                item.put("解析", analysis);
            }
            items.add(item);
        }
        root.put("题目列表", items);
        return JSONUtil.toJsonPrettyStr(root);
    }

    private String typeLabel(Integer quType) {
        if (quType == null) {
            return "未知";
        }
        switch (quType) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "简答题";
            case 5: return "复合题";
            default: return "未知";
        }
    }

    private String truncate(String text, int maxLen) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }
}
