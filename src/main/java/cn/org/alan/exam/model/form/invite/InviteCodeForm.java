package cn.org.alan.exam.model.form.invite;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 管理员生成邀请码表单。
 */
@Data
public class InviteCodeForm {

    @NotNull(message = "请选择邀请码对应身份")
    @Min(value = 2, message = "邀请码仅支持教师或管理员")
    @Max(value = 3, message = "邀请码仅支持教师或管理员")
    private Integer roleId;

    @NotNull(message = "请设置可用次数")
    @Min(value = 1, message = "可用次数至少为 1")
    @Max(value = 9999, message = "可用次数过大")
    private Integer maxUses;

    private String remark;

    /** 为空表示永不过期 */
    private LocalDateTime expireTime;
}
