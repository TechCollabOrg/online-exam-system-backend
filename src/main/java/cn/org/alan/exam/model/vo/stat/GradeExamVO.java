package cn.org.alan.exam.model.vo.stat;

import lombok.Data;

/**
 * 统计接口返回：某班级关联的考试场次或试卷数量等。
 *
 * @author JinXi
 * @since 2024/5/11
 */
@Data
public class GradeExamVO {
    private Integer id;
    // 班级名称
    private String gradeName;
    private Integer total;
}
