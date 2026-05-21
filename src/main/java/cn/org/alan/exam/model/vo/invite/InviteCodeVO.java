package cn.org.alan.exam.model.vo.invite;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码列表展示。
 */
@Data
public class InviteCodeVO {

    private Integer id;
    private String code;
    private Integer roleId;
    private String roleName;
    private Integer maxUses;
    private Integer usedCount;
    private Integer status;
    private String remark;
    private LocalDateTime expireTime;
    private String creatorName;
    private LocalDateTime createTime;
}
