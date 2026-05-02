package cn.org.alan.exam.model.form.userbook;

import lombok.Data;

import java.util.List;

/**
 * 错题本中再次作答某题时提交的答案与题目标识。
 *
 * @author Alan
 * @since 2024/4/25
 */
@Data
public class ReUserBookForm {
    // 试卷ID
    private Integer examId;

    // 试题ID
    private Integer quId;

    // 回答列表
    private String answer;
}
