package cn.org.alan.exam.model.form.grade;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建或编辑班级：名称、编码、关联教师等。
 *
 * @author Alan
 * @since 2024/3/28
 */
@Data
public class GradeForm {
    // 班级名称
    @NotBlank
    private String gradeName;

    // 班级口令
    private String code;
}
