package cn.org.alan.exam.model.form.proctor;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ProctorEventForm {

    @NotNull(message = "examId 不能为空")
    private Integer examId;

    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    private String detail;
}
