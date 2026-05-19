package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.AiPlatformConfigMapper;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.entity.AiPlatformConfig;
import cn.org.alan.exam.model.form.ai.AiConfigTestChatForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigProbeForm;
import cn.org.alan.exam.model.vo.ai.AiConfigStatusVO;
import cn.org.alan.exam.model.vo.ai.AiConnectionTestVO;
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
import java.util.List;

@Service
public class AiPlatformConfigServiceImpl extends ServiceImpl<AiPlatformConfigMapper, AiPlatformConfig>
        implements IAiPlatformConfigService {

    @Resource
    private AiPlatformConfigMapper aiPlatformConfigMapper;

    @Resource
    private LlmChatExecutor llmChatExecutor;

    @Override
    public Result<AiPlatformConfigVO> getConfigForAdmin() {
        AiPlatformConfig row = requireRow();
        return Result.success("ok", toVo(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveConfig(AiPlatformConfigForm form) {
        AiPlatformConfig row = requireRow();
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
    public Result<AiConnectionTestVO> testConnection(AiPlatformConfigProbeForm form) {
        String apiKey = resolveApiKeyForProbe(form.getApiKey());
        AiConnectionTestVO vo = new AiConnectionTestVO();
        try {
            List<String> models = LlmConnectionHelper.fetchModelIds(form.getBaseUrl(), apiKey);
            vo.setValid(true);
            vo.setMessage("连接成功");
            vo.setModels(models);
            touchTestResult(true);
            return Result.success("ok", vo);
        } catch (ServiceRuntimeException e) {
            vo.setValid(false);
            vo.setMessage(e.getMessage());
            touchTestResult(false);
            return Result.success("ok", vo);
        }
    }

    @Override
    public Result<List<String>> listModels(AiPlatformConfigProbeForm form) {
        String apiKey = resolveApiKeyForProbe(form.getApiKey());
        List<String> models = LlmConnectionHelper.fetchModelIds(form.getBaseUrl(), apiKey);
        return Result.success("ok", models);
    }

    @Override
    public Result<String> testChat(AiConfigTestChatForm form) {
        LlmResolvedConfig config = resolveActive();
        if (config == null) {
            return Result.failed("请先保存并启用配置，且填写完整端点、密钥与模型");
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
    public Result<AiConfigStatusVO> getPublicStatus() {
        LlmResolvedConfig active = resolveActive();
        AiConfigStatusVO vo = new AiConfigStatusVO();
        vo.setConfigured(active != null);
        vo.setModelName(active != null ? active.getModelName() : "");
        return Result.success("ok", vo);
    }

    @Override
    public LlmResolvedConfig resolveActive() {
        AiPlatformConfig row = aiPlatformConfigMapper.selectById(AiPlatformConfig.SINGLETON_ID);
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

    private AiPlatformConfig requireRow() {
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

    private String resolveApiKeyForProbe(String probeKey) {
        if (StringUtils.isNotBlank(probeKey)) {
            return probeKey.trim();
        }
        AiPlatformConfig row = requireRow();
        if (StringUtils.isBlank(row.getApiKey())) {
            throw new ServiceRuntimeException("请填写 API 密钥，或先保存密钥后再测试");
        }
        return row.getApiKey();
    }

    private void touchTestResult(boolean ok) {
        AiPlatformConfig row = requireRow();
        row.setLastTestOk(ok ? 1 : 0);
        row.setLastTestTime(LocalDateTime.now());
        aiPlatformConfigMapper.updateById(row);
    }

    private AiPlatformConfigVO toVo(AiPlatformConfig row) {
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
}
