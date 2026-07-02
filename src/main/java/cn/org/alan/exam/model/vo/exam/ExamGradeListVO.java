package cn.org.alan.exam.model.vo.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 按班级查看可参加考试的分页行：考试名、时间、状态。
 *
 * @author Alan
 * @since 2024/5/11
 */
@Data
public class ExamGradeListVO {
    /**
     * id    考试表
     */
    private Integer id;

    /**
     * 考试名称
     */
    private String title;

    /**
     * 考试时长
     */
    private Integer examDuration;

    /**
     * 及格分
     */
    private Double passedScore;

    /**
     * 总分数（展示分）
     */
    private Double grossScore;

    /**
     * 最大切屏次数
     */
    private Integer maxCount;

    /**
     * 创建者id
     */
    private Integer userId;

    /**
     * 证书id
     */
    private Integer certificateId;

    /**
     * 单选题数量
     */
    private Integer radioCount;

    /**
     * 单选题成绩     数据库存储*100，前端正常输入和展示/100
     */
    private Double radioScore;

    /**
     * 多选题数量
     */
    private Integer multiCount;

    /**
     * 多选题成绩     数据库存储*100，前端正常输入和展示/100
     */
    private Double multiScore;

    /**
     * 判断题数量
     */
    private Integer judgeCount;

    /**
     * 判断题成绩     数据库存储*100，前端正常输入和展示/100
     */
    private Double judgeScore;

    /**
     * 简答题数量
     */
    private Integer saqCount;

    /**
     * 简答题成绩（展示分）
     */
    private Double saqScore;

    /**
     * 复合题数量
     */
    private Integer compoundCount;

    /**
     * 复合题成绩（展示分）
     */
    private Double compoundScore;

    /**
     * 开始时间     YYYY-MM-DD hh:mm:ss
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间     YYYY-MM-DD hh:mm:ss
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 创建时间     YYYY-MM-DD hh:mm:ss
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
