package cn.org.alan.exam.model.vo.answer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 某场考试中尚未完成主观题阅卷的考生分页行。
 *
 * @author WeiJin
 * @since 2024/4/29
 */
@Data
public class UncorrectedUserVO {
    // 用户ID
    private Integer userId;
    // 用户名称
    private String userName;
    // 试卷标题
    private String examTitle;
    // 试卷ID
    private Integer examId;
    // 考试时间
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime limitTime;
    private String corrected;

}
