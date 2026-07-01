package cn.org.alan.exam.model.vo.proctor;

import lombok.Data;

@Data
public class ProctorTokenVO {

    private String livekitUrl;

    private String token;

    private String roomName;

    private String identity;
}
