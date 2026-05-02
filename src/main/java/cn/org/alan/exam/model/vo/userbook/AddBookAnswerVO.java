package cn.org.alan.exam.model.vo.userbook;

import lombok.Data;

/**
 * 错题复习作答提交后的判题结果（是否答对等）。
 *
 * @author Alan
 * @since 2024/4/28
 */
@Data
public class AddBookAnswerVO {
    // 返回是否正确
    private Integer correct;
    // 返回正确答案
    private String rightAnswers;
    // 返回分析
    private String analysis;
}
