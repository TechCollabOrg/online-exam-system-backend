package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiConfigTestChatForm;
import cn.org.alan.exam.model.form.ai.AiFeatureConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigForm;
import cn.org.alan.exam.model.form.ai.AiPlatformConfigProbeForm;
import cn.org.alan.exam.model.vo.ai.AiConfigOverviewVO;
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

@Api(tags = "AI API 连接配置")
@RestController
@RequestMapping("/api/ai/config")
public class AiConfigController {

    @Resource
    private IAiPlatformConfigService aiPlatformConfigService;

    @ApiOperation("配置总览（默认 + 各功能，管理员）")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<AiConfigOverviewVO> overview() {
        return aiPlatformConfigService.getOverviewForAdmin();
    }

    @ApiOperation("读取默认配置（管理员）")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<AiPlatformConfigVO> getConfig() {
        return aiPlatformConfigService.getConfigForAdmin();
    }

    @ApiOperation("保存默认配置（管理员）")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> saveConfig(@Validated @RequestBody AiPlatformConfigForm form) {
        return aiPlatformConfigService.saveConfig(form);
    }

    @ApiOperation("保存某功能的单独配置（管理员）")
    @PutMapping("/features/{featureCode}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> saveFeatureConfig(@PathVariable String featureCode,
                                            @Validated @RequestBody AiFeatureConfigForm form) {
        return aiPlatformConfigService.saveFeatureConfig(featureCode, form);
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

    @ApiOperation("发送测试消息（管理员）")
    @PostMapping("/test-chat")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> testChat(@Validated @RequestBody AiConfigTestChatForm form) {
        return aiPlatformConfigService.testChat(form);
    }

    @ApiOperation("AI 是否已配置（各角色可读）")
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<AiConfigStatusVO> status(@RequestParam(required = false) String feature) {
        return aiPlatformConfigService.getPublicStatus(feature);
    }
}
