package cn.org.alan.exam.model.vo.stat;

import lombok.Data;

/**
 * 统计接口返回：某班级学生人数等聚合。
 *
 * @author JinXi
 * @since 2024/5/12
 */
@Data
public class GradeStudentVO {
    private Long id;
    // 班级名称
    private String gradeName;
    // 班级下总学生数
    private Integer  totalStudent;

}
