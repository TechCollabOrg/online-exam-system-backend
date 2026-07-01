package cn.org.alan.exam.model.form.proctor;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class ProctorLeaveForm {

    @NotNull(message = "examId 不能为空")
    private Integer examId;

    @NotNull(message = "minutes 不能为空")
    @Min(value = 1, message = "暂离时长至少 1 分钟")
    private Integer minutes;
}
