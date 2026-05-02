package cn.org.alan.exam.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * Springfox Swagger2：仅扫描标注了 {@link ApiOperation} 的控制器方法生成文档；
 * 全局注入可选 Header {@code Authorization}，便于在线调试携带 JWT。
 *
 * @author Alan
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    /**
     * 构建 API 分组 {@link Docket}：Swagger 2 协议、绑定标题信息、全局 Token 参数。
     *
     * @return Swagger 文档入口 Bean
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .globalOperationParameters(parameters())
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 文档首页展示的标题、描述与版本号。
     *
     * @return {@link ApiInfo}
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("在线考试系统接口文档")
                .description("本接口文档阅读对象：WEB服务前后端开发人员")
                .version("1.0")
                .build();
    }

    /**
     * 全局请求头参数列表（当前仅 Authorization），各接口文档页均可填入 Bearer Token。
     *
     * @return 全局参数
     */
    private List<Parameter> parameters() {
        Parameter tokentPar = new ParameterBuilder()
                .name("Authorization")
                .description("认证token")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false)
                .build();

        List<Parameter> parameters = new ArrayList<>();
        parameters.add(tokentPar);
        return parameters;
    }
}