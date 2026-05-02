package cn.org.alan.exam.model.form.exam;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 整场考试交卷时聚合的多题作答明细项。
 *
 * @author Alan
 * @since 2024/4/7
 */
@Data
public class ExamQuAnswerFrom {
    // 试卷ID
    private Integer examId;
    // 试题ID
    private Integer quId;
    // 回答答案
    @NotBlank
    private String answer;
}
