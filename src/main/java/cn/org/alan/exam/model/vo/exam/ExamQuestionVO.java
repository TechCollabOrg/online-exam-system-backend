package cn.org.alan.exam.model.vo.exam;

import lombok.Data;

/**
 * 答题卡中单题项。
 */
@Data
public class ExamQuestionVO {

    private Integer questionId;

    private Integer sort;

    private Boolean checkout;
}
