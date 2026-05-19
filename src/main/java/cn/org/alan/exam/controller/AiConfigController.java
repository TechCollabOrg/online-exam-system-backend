package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiConfigTestChatForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigProbeForm;
import cn.org.alan.exam.model.vo.ai.AiConfigStatusVO;
import cn.org.alan.exam.model.vo.ai.AiConnectionTestVO;
import cn.org.alan.exam.model.vo.ai.AiPlatformConfigVO;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员维护 OpenAI 兼容 API；教师/学生通过其它 AI 接口间接使用库内配置。
 */
@Api(tags = "AI API 连接配置")
@RestController
@RequestMapping("/api/ai/config")
public class AiConfigController {

    @Resource
    private IAiPlatformConfigService aiPlatformConfigService;

    @ApiOperation("读取配置（管理员）")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<AiPlatformConfigVO> getConfig() {
        return aiPlatformConfigService.getConfigForAdmin();
    }

    @ApiOperation("保存配置（管理员）")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> saveConfig(@Validated @RequestBody AiPlatformConfigForm form) {
        return aiPlatformConfigService.saveConfig(form);
    }

    @ApiOperation("测试连接并拉取可用模型（管理员）")
    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<AiConnectionTestVO> testConnection(@Validated @RequestBody AiPlatformConfigProbeForm form) {
        return aiPlatformConfigService.testConnection(form);
    }

    @ApiOperation("拉取可用模型列表（管理员）")
    @PostMapping("/models")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<List<String>> listModels(@Validated @RequestBody AiPlatformConfigProbeForm form) {
        return aiPlatformConfigService.listModels(form);
    }

    @ApiOperation("发送测试消息（管理员，使用已保存且启用的配置）")
    @PostMapping("/test-chat")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> testChat(@Validated @RequestBody AiConfigTestChatForm form) {
        return aiPlatformConfigService.testChat(form);
    }

    @ApiOperation("AI 是否已由管理员配置（各角色可读，不含密钥）")
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<AiConfigStatusVO> status() {
        return aiPlatformConfigService.getPublicStatus();
    }
}
