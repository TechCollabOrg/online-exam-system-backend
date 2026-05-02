package cn.org.alan.exam.model.vo.exam;

import lombok.Data;

/**
 * 考试与绑定班级的简要关系（班级 id、名称等）。
 *
 * @author Alan
 * @since 2024/4/8
 */
@Data
public class ExamGradeVO {
    private Integer id;

    /**
     * 考试id  唯一
     */
    private Integer examId;

    /**
     * 班级id  唯一
     */
    private Integer gradeId;
}
