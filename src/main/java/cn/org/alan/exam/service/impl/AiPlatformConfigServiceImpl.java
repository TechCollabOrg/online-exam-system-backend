package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.enums.AiFeatureCode;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.AiFeatureConfigMapper;
import cn.org.alan.exam.mapper.AiPlatformConfigMapper;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.entity.AiFeatureConfig;
import cn.org.alan.exam.model.entity.AiPlatformConfig;
import cn.org.alan.exam.model.form.ai.AiConfigTestChatForm;
import cn.org.alan.exam.model.form.ai.AiFeatureConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigProbeForm;
import cn.org.alan.exam.model.vo.ai.AiConfigOverviewVO;
import cn.org.alan.exam.model.vo.ai.AiConfigStatusVO;
import cn.org.alan.exam.model.vo.ai.AiConnectionTestVO;
import cn.org.alan.exam.model.vo.ai.AiFeatureConfigVO;
import cn.org.alan.exam.model.vo.ai.AiPlatformConfigVO;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.agent.Constants;
import cn.org.alan.exam.utils.agent.LlmChatExecutor;
import cn.org.alan.exam.utils.agent.LlmConnectionHelper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiPlatformConfigServiceImpl extends ServiceImpl<AiPlatformConfigMapper, AiPlatformConfig>
        implements IAiPlatformConfigService {

    @Resource
    private AiPlatformConfigMapper aiPlatformConfigMapper;

    @Resource
    private AiFeatureConfigMapper aiFeatureConfigMapper;

    @Resource
    private LlmChatExecutor llmChatExecutor;

    @Override
    public Result<AiConfigOverviewVO> getOverviewForAdmin() {
        ensureFeatureRows();
        AiConfigOverviewVO overview = new AiConfigOverviewVO();
        overview.setDefaultConfig(toDefaultVo(requireDefaultRow()));
        List<AiFeatureConfigVO> features = new ArrayList<>();
        for (AiFeatureCode code : AiFeatureCode.values()) {
            features.add(toFeatureVo(code, requireFeatureRow(code)));
        }
        overview.setFeatures(features);
        return Result.success("ok", overview);
    }

    @Override
    public Result<AiPlatformConfigVO> getConfigForAdmin() {
        return Result.success("ok", toDefaultVo(requireDefaultRow()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveConfig(AiPlatformConfigForm form) {
        AiPlatformConfig row = requireDefaultRow();
        row.setBaseUrl(LlmConnectionHelper.normalizeBaseUrl(form.getBaseUrl()));
        if (StringUtils.isNotBlank(form.getApiKey())) {
            row.setApiKey(form.getApiKey().trim());
        } else if (StringUtils.isBlank(row.getApiKey())) {
            throw new ServiceRuntimeException("首次保存请填写 API 密钥");
        }
        row.setModelName(form.getModelName().trim());
        row.setEnabled(Boolean.TRUE.equals(form.getEnabled()) ? 1 : 0);
        row.setUpdateUserId(SecurityUtil.getUserId());
        row.setUpdateTime(LocalDateTime.now());
        aiPlatformConfigMapper.updateById(row);
        return Result.success("保存成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveFeatureConfig(String featureCode, AiFeatureConfigForm form) {
        AiFeatureCode code = requireFeatureCode(featureCode);
        AiFeatureConfig row = requireFeatureRow(code);
        boolean useDefault = Boolean.TRUE.equals(form.getUseDefault());
        row.setUseDefault(useDefault ? 1 : 0);
        if (useDefault) {
            row.setUpdateUserId(SecurityUtil.getUserId());
            row.setUpdateTime(LocalDateTime.now());
            aiFeatureConfigMapper.updateById(row);
            return Result.success("已设为沿用默认连接");
        }
        row.setBaseUrl(LlmConnectionHelper.normalizeBaseUrl(form.getBaseUrl()));
        if (StringUtils.isNotBlank(form.getApiKey())) {
            row.setApiKey(form.getApiKey().trim());
        } else if (StringUtils.isBlank(row.getApiKey())) {
            throw new ServiceRuntimeException("单独配置时请填写 API 密钥");
        }
        row.setModelName(form.getModelName().trim());
        row.setEnabled(Boolean.TRUE.equals(form.getEnabled()) ? 1 : 0);
        row.setUpdateUserId(SecurityUtil.getUserId());
        row.setUpdateTime(LocalDateTime.now());
        aiFeatureConfigMapper.updateById(row);
        return Result.success("保存成功");
    }

    @Override
    public Result<AiConnectionTestVO> testConnection(AiPlatformConfigProbeForm form) {
        String apiKey = resolveApiKeyForProbe(form);
        AiConnectionTestVO vo = new AiConnectionTestVO();
        try {
            List<String> models = LlmConnectionHelper.fetchModelIds(form.getBaseUrl(), apiKey);
            vo.setValid(true);
            vo.setMessage("连接成功");
            vo.setModels(models);
            if (StringUtils.isBlank(form.getFeatureCode())) {
                touchDefaultTestResult(true);
            }
            return Result.success("ok", vo);
        } catch (ServiceRuntimeException e) {
            vo.setValid(false);
            vo.setMessage(e.getMessage());
            if (StringUtils.isBlank(form.getFeatureCode())) {
                touchDefaultTestResult(false);
            }
            return Result.success("ok", vo);
        }
    }

    @Override
    public Result<List<String>> listModels(AiPlatformConfigProbeForm form) {
        String apiKey = resolveApiKeyForProbe(form);
        List<String> models = LlmConnectionHelper.fetchModelIds(form.getBaseUrl(), apiKey);
        return Result.success("ok", models);
    }

    @Override
    public Result<String> testChat(AiConfigTestChatForm form) {
        LlmResolvedConfig config;
        if (StringUtils.isNotBlank(form.getFeatureCode())) {
            AiFeatureCode code = requireFeatureCode(form.getFeatureCode());
            config = resolveForFeature(code);
        } else {
            config = resolveActive();
        }
        if (config == null) {
            return Result.failed("请先保存并启用对应配置，且填写完整端点、密钥与模型");
        }
        try {
            String reply = llmChatExecutor.chat(
                    config,
                    "你是连接测试助手，请用一句话确认收到消息。",
                    form.getMessage(),
                    Constants.temperature);
            return Result.success("ok", reply != null ? reply : "");
        } catch (Exception e) {
            return Result.failed("测试消息发送失败：" + e.getMessage());
        }
    }

    @Override
    public Result<AiConfigStatusVO> getPublicStatus(String featureCode) {
        LlmResolvedConfig active;
        String codeStr = null;
        if (StringUtils.isNotBlank(featureCode)) {
            AiFeatureCode code = AiFeatureCode.fromCode(featureCode);
            if (code == null) {
                return Result.failed("未知的功能编码");
            }
            codeStr = code.getCode();
            active = resolveForFeature(code);
        } else {
            active = resolveActive();
        }
        AiConfigStatusVO vo = new AiConfigStatusVO();
        vo.setConfigured(active != null);
        vo.setModelName(active != null ? active.getModelName() : "");
        vo.setFeatureCode(codeStr);
        return Result.success("ok", vo);
    }

    @Override
    public LlmResolvedConfig resolveActive() {
        AiPlatformConfig row = aiPlatformConfigMapper.selectById(AiPlatformConfig.SINGLETON_ID);
        return toResolved(row);
    }

    @Override
    public LlmResolvedConfig resolveForFeature(AiFeatureCode feature) {
        if (feature == null) {
            return resolveActive();
        }
        ensureFeatureRows();
        AiFeatureConfig row = aiFeatureConfigMapper.selectById(feature.getCode());
        if (row == null || row.getUseDefault() != null && row.getUseDefault() == 1) {
            return resolveActive();
        }
        return toResolved(row);
    }

    private LlmResolvedConfig toResolved(AiPlatformConfig row) {
        if (row == null || row.getEnabled() == null || row.getEnabled() != 1) {
            return null;
        }
        if (StringUtils.isBlank(row.getBaseUrl())
                || StringUtils.isBlank(row.getApiKey())
                || StringUtils.isBlank(row.getModelName())) {
            return null;
        }
        return LlmResolvedConfig.of(
                LlmConnectionHelper.normalizeBaseUrl(row.getBaseUrl()),
                row.getApiKey(),
                row.getModelName(),
                true);
    }

    private LlmResolvedConfig toResolved(AiFeatureConfig row) {
        if (row == null || row.getEnabled() == null || row.getEnabled() != 1) {
            return null;
        }
        if (StringUtils.isBlank(row.getBaseUrl())
                || StringUtils.isBlank(row.getApiKey())
                || StringUtils.isBlank(row.getModelName())) {
            return null;
        }
        return LlmResolvedConfig.of(
                LlmConnectionHelper.normalizeBaseUrl(row.getBaseUrl()),
                row.getApiKey(),
                row.getModelName(),
                true);
    }

    private AiPlatformConfig requireDefaultRow() {
        AiPlatformConfig row = aiPlatformConfigMapper.selectById(AiPlatformConfig.SINGLETON_ID);
        if (row == null) {
            row = new AiPlatformConfig();
            row.setId(AiPlatformConfig.SINGLETON_ID);
            row.setBaseUrl("");
            row.setApiKey("");
            row.setModelName("");
            row.setEnabled(0);
            aiPlatformConfigMapper.insert(row);
        }
        return row;
    }

    private void ensureFeatureRows() {
        for (AiFeatureCode code : AiFeatureCode.values()) {
            requireFeatureRow(code);
        }
    }

    private AiFeatureConfig requireFeatureRow(AiFeatureCode code) {
        AiFeatureConfig row = aiFeatureConfigMapper.selectById(code.getCode());
        if (row == null) {
            row = new AiFeatureConfig();
            row.setFeatureCode(code.getCode());
            row.setUseDefault(1);
            row.setBaseUrl("");
            row.setApiKey("");
            row.setModelName("");
            row.setEnabled(0);
            aiFeatureConfigMapper.insert(row);
        }
        return row;
    }

    private AiFeatureCode requireFeatureCode(String featureCode) {
        AiFeatureCode code = AiFeatureCode.fromCode(featureCode);
        if (code == null) {
            throw new ServiceRuntimeException("未知的功能编码：" + featureCode);
        }
        return code;
    }

    private String resolveApiKeyForProbe(AiPlatformConfigProbeForm form) {
        if (StringUtils.isNotBlank(form.getApiKey())) {
            return form.getApiKey().trim();
        }
        if (StringUtils.isNotBlank(form.getFeatureCode())) {
            AiFeatureCode code = AiFeatureCode.fromCode(form.getFeatureCode());
            if (code != null) {
                AiFeatureConfig row = aiFeatureConfigMapper.selectById(code.getCode());
                if (row != null && StringUtils.isNotBlank(row.getApiKey())) {
                    return row.getApiKey();
                }
            }
        }
        AiPlatformConfig row = requireDefaultRow();
        if (StringUtils.isBlank(row.getApiKey())) {
            throw new ServiceRuntimeException("请填写 API 密钥，或先保存密钥后再测试");
        }
        return row.getApiKey();
    }

    private void touchDefaultTestResult(boolean ok) {
        AiPlatformConfig row = requireDefaultRow();
        row.setLastTestOk(ok ? 1 : 0);
        row.setLastTestTime(LocalDateTime.now());
        aiPlatformConfigMapper.updateById(row);
    }

    private AiPlatformConfigVO toDefaultVo(AiPlatformConfig row) {
        AiPlatformConfigVO vo = new AiPlatformConfigVO();
        vo.setBaseUrl(row.getBaseUrl());
        vo.setApiKeySet(StringUtils.isNotBlank(row.getApiKey()));
        vo.setModelName(row.getModelName());
        vo.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        vo.setLastTestOk(row.getLastTestOk() != null && row.getLastTestOk() == 1);
        vo.setLastTestTime(row.getLastTestTime());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }

    private AiFeatureConfigVO toFeatureVo(AiFeatureCode code, AiFeatureConfig row) {
        AiFeatureConfigVO vo = new AiFeatureConfigVO();
        vo.setFeatureCode(code.getCode());
        vo.setFeatureLabel(code.getLabel());
        vo.setFeatureHint(featureHint(code));
        vo.setUseDefault(row.getUseDefault() == null || row.getUseDefault() == 1);
        vo.setBaseUrl(row.getBaseUrl());
        vo.setApiKeySet(StringUtils.isNotBlank(row.getApiKey()));
        vo.setModelName(row.getModelName());
        vo.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        return vo;
    }

    private static String featureHint(AiFeatureCode code) {
        switch (code) {
            case GRADING:
                return "教师阅卷页「AI 阅卷」、交卷后自动主观题评分";
            case ASSISTANT:
                return "首页 AI 使用助手（含多轮对话）";
            case BRIEFING:
                return "教师成绩分析页的 AI 简报";
            case QUESTION_REVIEW:
                return "学生考试记录详情「AI 解析本题」";
            case QUESTION_IMPORT:
                return "试题管理页「AI 智能导入」（docx/md 转 JSON 并入库）";
            default:
                return "";
        }
    }
}
