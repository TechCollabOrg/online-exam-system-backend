package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.form.exam.ExamAddForm;
import cn.org.alan.exam.model.form.exam.ExamUpdateForm;
import cn.org.alan.exam.model.vo.exam.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 考试主数据、试卷题目关联及选项等多层结构的 MapStruct 转换。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel="spring")
public interface ExamConverter {

    /** 分页考试实体转简要 VO（列表页）。 */
    Page<ExamVO> pageEntityToVo(Page<Exam> examPage);

    /** 编辑表单覆盖到 {@link Exam} 实体字段。 */
    Exam  formToEntity(ExamUpdateForm examUpdateForm);

    /** 新增表单构建 {@link Exam} 实体。 */
    Exam  formToEntity(ExamAddForm examAddForm);

    /** 试卷下题目关联列表转阅卷/详情用扁平结构。 */
    List<ExamDetailRespVO> listEntityToExamDetailRespVO(List<ExamQuestion> examQuestion);

    /** 单条考试实体转详情页聚合根。 */
    ExamDetailVO examToExamDetailVO(Exam exam);

    /** 考试实体转成绩列表行展示对象。 */
    ExamGradeListVO entityToExamGradeListVO(Exam exam);

    /** 试卷题目关联转前端题目卡片 VO。 */
    ExamQuestionVO examQuestionEntityToVO(ExamQuestion examQuestion);

    /** 批量题目关联转 VO 列表。 */
    List<ExamQuestionVO> examQuestionListEntityToVO(List<ExamQuestion> examQuestion);

    /** 选项实体列表转选项 VO（展示题干选项文本等）。 */
    List<OptionVO> opListEntityToVO(List<Option> examQuestion);
}
