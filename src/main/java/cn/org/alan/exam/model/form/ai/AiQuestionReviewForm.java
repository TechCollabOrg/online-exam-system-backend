package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 学生考后单题 AI 解析请求。
 */
@Data
@ApiModel("AI 单题解析请求")
public class AiQuestionReviewForm {

    @NotNull
    @ApiModelProperty(value = "考试 ID", required = true)
    private Integer examId;

    @NotNull
    @ApiModelProperty(value = "题目 ID", required = true)
    private Integer quId;

    @ApiModelProperty("考生用户 ID；学生端不传则取当前登录用户，教师查看学生答卷时可传")
    private Integer userId;

    @ApiModelProperty("复合题子题序号（从 0 开始）；不传则分析整道复合题")
    private Integer subIndex;
}
