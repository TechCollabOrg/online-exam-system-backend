package cn.org.alan.exam.model.vo.exam;

import lombok.Data;

/**
 * 学生进入考试前看到的考试说明与注意事项聚合。
 *
 * @author Alan
 * @since 2024/4/1
 */
@Data
public class ExamDetailRespVO {
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
}
