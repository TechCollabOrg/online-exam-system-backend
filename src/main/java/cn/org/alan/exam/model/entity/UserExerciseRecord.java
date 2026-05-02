package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户在某个题库上的刷题汇总（进度/次数等维度），与单次题目作答 {@link ExerciseRecord} 不同。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
@Data
@ApiModel("用户刷题记录实体类")
@TableName("t_user_exercise_record")
public class UserExerciseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户刷题记录表ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("用户ID")
    @TableField(fill = FieldFill.INSERT)
    private Integer userId;

    @ApiModelProperty("题库ID")
    private Integer repoId;

    @ApiModelProperty("总题数")
    private Integer totalCount;

    @ApiModelProperty("已刷题数")
    private Integer exerciseCount;

    @ApiModelProperty("刷题时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
