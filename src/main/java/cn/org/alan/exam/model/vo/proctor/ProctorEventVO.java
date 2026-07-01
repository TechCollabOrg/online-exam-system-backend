package cn.org.alan.exam.model.vo.proctor;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProctorEventVO {

    private Long id;

    private Integer examId;

    private Integer userId;

    private String userName;

    private String eventType;

    private String detail;

    private LocalDateTime createTime;
}
