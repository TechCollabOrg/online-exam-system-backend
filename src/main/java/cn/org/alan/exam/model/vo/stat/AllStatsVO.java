package cn.org.alan.exam.model.vo.stat;

import lombok.Data;

/**
 * 首页或大屏汇总：班级数、考试数、用户活跃度等总览指标。
 *
 * @author JinXi
 * @since 2024/5/12
 */
@Data
public class AllStatsVO {
    // 班级数量
    private Integer classCount;
    // 试卷数量
    private Integer examCount;
    // 试题数量
    private Integer questionCount;


}
