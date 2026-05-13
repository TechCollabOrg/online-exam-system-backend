package cn.org.alan.exam.model.vo.userbook;

import cn.org.alan.exam.model.entity.Option;
import lombok.Data;

import java.util.List;

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
    /** 本题全部选项（含各选项解析、多图 URL），便于错题复习页展示 */
    private List<Option> options;
}
