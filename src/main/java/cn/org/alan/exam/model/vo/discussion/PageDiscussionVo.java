package cn.org.alan.exam.model.vo.discussion;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 讨论区分页列表行：标题、班级、回复数、时间等。
 *
 * @author WeiJin
 * @since 2025/4/4
 */
@Data
public class PageDiscussionVo {
    private Integer id;
    private String title;
    private String sender;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
