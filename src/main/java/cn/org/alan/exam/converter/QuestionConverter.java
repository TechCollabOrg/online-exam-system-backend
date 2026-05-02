package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.form.question.QuestionFrom;
import cn.org.alan.exam.model.vo.question.QuestionVO;
import cn.org.alan.exam.model.vo.exercise.QuestionSheetVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 试题表单、答题卡展示与通用题目 VO 之间的映射。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel = "spring")
public interface QuestionConverter {

    /**
     * 后台录入/编辑题目：表单转 {@link Question}。
     */
    @Mapping(target = "repoId",source = "repoId")
    Question fromToEntity(QuestionFrom questionFrom);

    /** 题目实体列表转答题卡简略信息列表。 */
    List<QuestionSheetVO> listEntityToVO(List<Question> questions);

    /**
     * 单题转答题卡 VO，题干主键映射为 {@code quId}。
     */
    @Mapping(target = "quId",source = "id")
    QuestionSheetVO entityToVO(Question question);

    /** 题目实体转通用题目详情 VO（含选项等扩展字段由 MapStruct 按同名映射）。 */
    QuestionVO QuestionToQuestionVO(Question question);
}
