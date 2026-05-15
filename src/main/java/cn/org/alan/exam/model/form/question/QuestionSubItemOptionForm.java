package cn.org.alan.exam.model.form.question;

import lombok.Data;

/**
 * 复合题小题内的选项或填空参考答案。
 */
@Data
public class QuestionSubItemOptionForm {

    private Integer id;

    private String content;

    private String image;

    private String analysis;

    /** 0 错误 1 正确 */
    private Integer isRight;
}
