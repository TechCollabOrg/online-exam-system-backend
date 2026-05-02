package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Grade;
import cn.org.alan.exam.model.form.grade.GradeForm;
import cn.org.alan.exam.model.vo.grade.GradeVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 班级（Grade）实体与表单、列表/分页 VO 映射。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel="spring")
public interface GradeConverter {

    /** 班级分页实体转 VO。 */
    Page<GradeVO> pageEntityToVo(Page<Grade> page);

    /** 新增/编辑班级表单转实体。 */
    Grade formToEntity(GradeForm gradeForm);

    /** 班级列表实体批量转 VO（如导出或下拉）。 */
    List<GradeVO> listEntityToVo(List<Grade> page);

    /** 单条班级实体转详情/卡片 VO。 */
    GradeVO  GradeToGradeVO(Grade grade);

}
