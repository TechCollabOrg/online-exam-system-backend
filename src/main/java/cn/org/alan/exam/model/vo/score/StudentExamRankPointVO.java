package cn.org.alan.exam.model.vo.score;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生端：单场考试在班级中的名次与分数，用于按「学科」维度绘制排名变化曲线。
 */
@Data
public class StudentExamRankPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer examId;
    /** 试卷名称（完整标题） */
    private String examTitle;
    /**
     * 从试卷标题解析的学科/课程维度（如「数学-月考」→「数学」），用于多条曲线分组。
     */
    private String subjectLabel;

    private Integer userScore;
    /** 试卷满分（总分） */
    private Integer grossScore;
    /** 班级内名次，1 表示最高 */
    private Integer rankInClass;
    /** 该场考试本班有成绩记录的人数 */
    private Integer classAttendCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime examTime;
}
