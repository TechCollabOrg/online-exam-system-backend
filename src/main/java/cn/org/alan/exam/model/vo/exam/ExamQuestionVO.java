package cn.org.alan.exam.model.vo.exam;

import lombok.Data;

/**
 * 组卷或考试题目列表中的单题项：题型、分值、排序、题干摘要。
 *
 * @author Alan
 * @since 2024/5/20
 */
@Data
public class ExamQuestionVO {
    private Integer id;
    /**
     * 考试id  唯一
     */
    private Integer examId;

    /**
     * 试题id  唯一
     */
    private Integer questionId;
    /**
     * 分数
     */
    private Integer score;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 类型
     */
    private Integer type;
    /**
     * 类型
     */
    private Boolean checkout;

    /** 若非空，本题与卷内其它同 parentQuId 的小题共用该父题题干 */
    private Integer parentQuId;
}
