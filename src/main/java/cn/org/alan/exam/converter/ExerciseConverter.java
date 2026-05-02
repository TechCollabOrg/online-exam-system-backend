package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.ExerciseRecord;
import cn.org.alan.exam.model.form.exercise.ExerciseFillAnswerFrom;
import cn.org.alan.exam.model.vo.question.QuestionVO;
import cn.org.alan.exam.model.vo.exercise.AnswerInfoVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.stereotype.Component;

/**
 * 练习作答提交与题目 VO 到答题信息展示的映射。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel="spring")
public interface ExerciseConverter {
    /**
     * 练习填空/选项提交表单转答题流水：题干 ID、题型字段分别映射为 {@code questionId}/{@code questionType}。
     */
    @Mappings({
            @Mapping(source = "quId",target = "questionId"),
            @Mapping(source = "quType",target = "questionType")
    })
    ExerciseRecord fromToEntity(ExerciseFillAnswerFrom exerciseFillAnswerFrom);

    /** 将通用 {@link QuestionVO} 转为练习模块所需的答题信息结构。 */
    AnswerInfoVO quVOToAnswerInfoVO(QuestionVO questionVO);
}
