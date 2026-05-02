package cn.org.alan.exam.model.form.exam_qu_answer;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 考试过程中暂存或提交单题答案的入参结构。
 *
 * @author Alan
 * @since 2024/5/6
 */
@Data
public class ExamQuAnswerAddForm {
    // 试卷ID
    private Integer examId;
    // 试题ID
    private Integer quId;
    // 回答答案
    @NotBlank
    private String answer;
}
