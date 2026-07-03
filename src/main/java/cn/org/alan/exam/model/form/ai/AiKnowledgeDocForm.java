package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel("AI 知识库文档")
public class AiKnowledgeDocForm {

    @NotBlank(message = "请填写标题")
    @Size(max = 200, message = "标题不超过 200 字")
    private String title;

    @NotBlank(message = "请填写正文")
    private String content;

    private Boolean enabled;

    private Integer sortOrder;
}
