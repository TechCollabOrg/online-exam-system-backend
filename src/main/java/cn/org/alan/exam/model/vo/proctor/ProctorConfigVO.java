package cn.org.alan.exam.model.vo.proctor;

import lombok.Data;

@Data
public class ProctorConfigVO {

    private Integer proctorEnabled;

    private Integer allowLeave;

    private Integer leaveMaxMinutes;

    private Integer leaveMaxCount;
}
