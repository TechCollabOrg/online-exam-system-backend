package cn.org.alan.exam.model.vo.score;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mapper 中间行：当前学生已完成的考试得分。
 */
@Data
public class MyExamScoreRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer examId;
    private String examTitle;
    /** 试卷满分 */
    private Integer grossScore;
    private Integer userScore;
    private LocalDateTime limitTime;
}
