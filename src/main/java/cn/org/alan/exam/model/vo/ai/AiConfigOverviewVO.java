package cn.org.alan.exam.model.vo.ai;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("AI 配置总览")
public class AiConfigOverviewVO {

    private AiPlatformConfigVO defaultConfig;

    private List<AiFeatureConfigVO> features;
}
