package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.form.ai.AiConfigTestChatForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigProbeForm;
import cn.org.alan.exam.model.vo.ai.AiConfigStatusVO;
import cn.org.alan.exam.model.vo.ai.AiConnectionTestVO;
import cn.org.alan.exam.model.vo.ai.AiPlatformConfigVO;

import java.util.List;

/**
 * 管理员维护的 AI API 连接配置。
 */
public interface IAiPlatformConfigService {

    /** 管理员读取配置（密钥脱敏） */
    Result<AiPlatformConfigVO> getConfigForAdmin();

    /** 保存配置 */
    Result<String> saveConfig(AiPlatformConfigForm form);

    /** 测试连接并返回可用模型 */
    Result<AiConnectionTestVO> testConnection(AiPlatformConfigProbeForm form);

    /** 仅拉取模型列表 */
    Result<List<String>> listModels(AiPlatformConfigProbeForm form);

    /** 使用当前已保存配置发送测试消息 */
    Result<String> testChat(AiConfigTestChatForm form);

    /** 各角色可读：是否已配置 */
    Result<AiConfigStatusVO> getPublicStatus();

    /**
     * 解析当前生效的库内配置；未启用或字段不全时返回 null。
     */
    LlmResolvedConfig resolveActive();
}
