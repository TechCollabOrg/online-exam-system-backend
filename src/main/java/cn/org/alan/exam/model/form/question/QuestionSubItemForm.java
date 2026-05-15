package cn.org.alan.exam.model.form.question;

import lombok.Data;

import java.util.List;

/**
 * 复合题（题型 5）中的单道小题：可有独立题型与多个选项/填空。
 */
@Data
public class QuestionSubItemForm {

    private Integer sort;

    /** 小题题干，如 (1)… */
    private String content;

    /** 1单选 2多选 3判断 4简答 */
    private Integer quType;

    private List<QuestionSubItemOptionForm> options;
}
