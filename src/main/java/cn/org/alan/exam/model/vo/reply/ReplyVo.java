package cn.org.alan.exam.model.vo.reply;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 讨论回复列表展示：作者、内容、点赞数、排序字段等。
 *
 * @author WeiJin
 * @since 2025/4/4
 */
@Data
public class ReplyVo {
    private Integer id;
    private Integer userId;
    private Integer parentId;
    private Integer discussionId;
    private String avatar;
    private String content;
    private String realName;
    private Integer count;
    private Integer isLike;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<ReplyVo> childReplies;
}
