package cn.org.alan.exam.model.vo.exam;

import cn.org.alan.exam.model.entity.Option;
import lombok.Data;

import java.util.List;

/**
 * 进入考试后单题完整内容：题干、选项、已选状态、标记等。
 *
 * @author Alan
 * @since 2024/4/1
 */
@Data
public class ExamQuDetailVO {
    private static final long serialVersionUID = 1L;

    /**
     * 图片
     */
    private String image;

    /**
     * 题目内容
     */
    private String content;
    /**
     * 答案内容
     */
    private List<OptionVO> answerList;
    // 试题类型
    private Integer quType;
    /**
     * 排序
     */
    private Integer sort;

}
