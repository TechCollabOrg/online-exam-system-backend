package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 注册邀请码：限定教师/管理员自助注册。
 */
@Data
@ApiModel("邀请码实体")
@TableName("t_invite_code")
public class InviteCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("邀请码")
    private String code;

    @ApiModelProperty("目标角色：2教师 3管理员")
    private Integer roleId;

    @ApiModelProperty("最大可用次数")
    private Integer maxUses;

    @ApiModelProperty("已使用次数")
    private Integer usedCount;

    @ApiModelProperty("1启用 0禁用")
    private Integer status;

    private String remark;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
