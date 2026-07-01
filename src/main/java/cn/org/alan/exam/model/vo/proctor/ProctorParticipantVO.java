package cn.org.alan.exam.model.vo.proctor;

import lombok.Data;

@Data
public class ProctorParticipantVO {

    private Integer userId;

    private String userName;

    private String realName;

    private String livekitIdentity;

    private Boolean online;

    private Boolean inLeave;
}
