package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.form.exam_qu_answer.ExamQuAnswerAddForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * 考试中单题作答提交 {@link ExamQuAnswerAddForm} 与 {@link ExamQuAnswer} 映射。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel = "spring")
public interface ExamQuAnswerConverter {

    /**
     * 前端提交 {@code quId} 对齐持久层 {@code questionId}。
     */
    @Mapping(target = "questionId", source = "quId")
    ExamQuAnswer formToEntity(ExamQuAnswerAddForm examQuAnswerAddForm);

}
