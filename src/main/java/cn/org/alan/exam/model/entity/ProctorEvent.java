package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_proctor_event")
public class ProctorEvent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer examId;

    private Integer userId;

    private String eventType;

    private String detail;

    private LocalDateTime createTime;
}
