package cn.org.alan.exam.model.form.like;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 对讨论或回复执行点赞/取消时提交的目标 ID 与类型。
 *
 * @author WeiJin
 * @since 2025/4/16
 */
@Data
public class LikeForm {
    @NotNull(message = "讨论id不能为空")
    private Integer discussionId;

    @NotNull(message = "回复id不能为空")
    private Integer replyId;
}
