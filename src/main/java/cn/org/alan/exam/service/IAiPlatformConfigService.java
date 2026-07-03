package cn.org.alan.exam.service;

import cn.org.alan.exam.common.enums.AiFeatureCode;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.form.ai.AiConfigTestChatForm;
import cn.org.alan.exam.model.form.ai.AiFeatureConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigProbeForm;
import cn.org.alan.exam.model.vo.ai.AiConfigOverviewVO;
import cn.org.alan.exam.model.vo.ai.AiConfigStatusVO;
import cn.org.alan.exam.model.vo.ai.AiConnectionTestVO;
import cn.org.alan.exam.model.vo.ai.AiFeatureConfigVO;
import cn.org.alan.exam.model.vo.ai.AiPlatformConfigVO;

import java.util.List;

/**
 * 管理员维护的 AI API 连接配置（默认 + 分功能）。
 */
public interface IAiPlatformConfigService {

    Result<AiConfigOverviewVO> getOverviewForAdmin();

    Result<AiPlatformConfigVO> getConfigForAdmin();

    Result<String> saveConfig(AiPlatformConfigForm form);

    Result<String> saveFeatureConfig(String featureCode, AiFeatureConfigForm form);

    Result<AiConnectionTestVO> testConnection(AiPlatformConfigProbeForm form);

    Result<List<String>> listModels(AiPlatformConfigProbeForm form);

    Result<String> testChat(AiConfigTestChatForm form);

    Result<AiConfigStatusVO> getPublicStatus(String featureCode);

    LlmResolvedConfig resolveActive();

    LlmResolvedConfig resolveForFeature(AiFeatureCode feature);
}
