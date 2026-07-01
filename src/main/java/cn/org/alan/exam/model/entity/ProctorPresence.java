package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_proctor_presence")
public class ProctorPresence implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer examId;

    private Integer userId;

    private LocalDateTime lastHeartbeat;
}
