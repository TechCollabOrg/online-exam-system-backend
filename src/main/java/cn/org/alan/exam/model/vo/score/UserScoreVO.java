package cn.org.alan.exam.model.vo.score;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成绩列表中单名学生的得分、排名与阅卷状态等。
 *
 * @author WeiJin
 * @since 2024/4/15
 */
@Data
public class UserScoreVO {
    private Integer id;
    // 用户ID
    private Integer userId;
    private String title;
    // 真实姓名
    private  String realName;
    private Integer userTime;
    // 用户分数
    private Integer userScore;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime limitTime;
    private Integer count;
    private Integer examId;

}
