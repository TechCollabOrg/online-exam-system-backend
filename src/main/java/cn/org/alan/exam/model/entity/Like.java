package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户对讨论或回复的点赞记录（防重复由业务与唯一约束保证）。
 *
 * @author WeiJin
 * @since 2025/4/16
 */
@Data
@TableName("t_like")
@ApiModel("点赞实体类")
public class Like implements Serializable {

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("讨论id")
    private Integer discussionId;

    @ApiModelProperty("用户id")
    private Integer userId;

    @ApiModelProperty("回复id")
    private Integer replyId;

    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
