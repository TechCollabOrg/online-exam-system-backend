package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_proctor_leave")
public class ProctorLeave implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer examId;

    private Integer userId;

    private Integer minutes;

    private LocalDateTime startTime;

    private LocalDateTime expectedEnd;

    private LocalDateTime actualEnd;

    /** 0 暂离中 1 已返回 2 超时 */
    private Integer status;

    private LocalDateTime createTime;
}
