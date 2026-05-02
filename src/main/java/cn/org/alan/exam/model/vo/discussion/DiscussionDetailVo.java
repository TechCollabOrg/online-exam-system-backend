package cn.org.alan.exam.model.vo.discussion;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 讨论详情页：主题正文、作者、可见范围及回复区入口数据。
 *
 * @author WeiJin
 * @since 2025/4/4
 */
@Data
public class DiscussionDetailVo {
    private Integer id;
    private String title;
    private String sender;
    private String content;
    private String gradeName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
