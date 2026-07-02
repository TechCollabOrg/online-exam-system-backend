package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.form.exam.ExamAddForm;
import cn.org.alan.exam.model.form.exam.ExamUpdateForm;
import cn.org.alan.exam.model.vo.exam.*;
import cn.org.alan.exam.utils.ExamScoreUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 考试主数据、试卷题目关联及选项等多层结构的 MapStruct 转换。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel = "spring")
public interface ExamConverter {

    @Mapping(target = "passedScore", ignore = true)
    @Mapping(target = "grossScore", ignore = true)
    @Mapping(target = "radioScore", ignore = true)
    @Mapping(target = "multiScore", ignore = true)
    @Mapping(target = "judgeScore", ignore = true)
    @Mapping(target = "saqScore", ignore = true)
    @Mapping(target = "compoundScore", ignore = true)
    ExamVO examToExamVo(Exam exam);

    /** 分页考试实体转简要 VO（列表页）。 */
    default Page<ExamVO> pageEntityToVo(Page<Exam> examPage) {
        if (examPage == null) {
            return null;
        }
        Page<ExamVO> voPage = new Page<>(examPage.getCurrent(), examPage.getSize(), examPage.getTotal());
        voPage.setRecords(examPage.getRecords().stream()
                .map(this::examToExamVo)
                .collect(java.util.stream.Collectors.toList()));
        return voPage;
    }

    @AfterMapping
    default void fillExamDisplayScores(Exam source, @MappingTarget ExamVO target) {
        if (source == null || target == null) {
            return;
        }
        target.setPassedScore(ExamScoreUtil.toDisplayDouble(source.getPassedScore()));
        target.setGrossScore(ExamScoreUtil.toDisplayDouble(source.getGrossScore()));
        target.setRadioScore(ExamScoreUtil.toDisplayDouble(source.getRadioScore()));
        target.setMultiScore(ExamScoreUtil.toDisplayDouble(source.getMultiScore()));
        target.setJudgeScore(ExamScoreUtil.toDisplayDouble(source.getJudgeScore()));
        target.setSaqScore(ExamScoreUtil.toDisplayDouble(source.getSaqScore()));
        target.setCompoundScore(ExamScoreUtil.toDisplayDouble(source.getCompoundScore()));
    }

    /** 编辑表单覆盖到 {@link Exam} 实体字段。 */
    Exam formToEntity(ExamUpdateForm examUpdateForm);

    /** 新增表单构建 {@link Exam} 实体。 */
    Exam formToEntity(ExamAddForm examAddForm);

    @Mapping(target = "passedScore", ignore = true)
    @Mapping(target = "grossScore", ignore = true)
    @Mapping(target = "radioScore", ignore = true)
    @Mapping(target = "multiScore", ignore = true)
    @Mapping(target = "judgeScore", ignore = true)
    @Mapping(target = "saqScore", ignore = true)
    @Mapping(target = "compoundScore", ignore = true)
    ExamDetailVO examToExamDetailVO(Exam exam);

    @AfterMapping
    default void fillExamDetailDisplayScores(Exam source, @MappingTarget ExamDetailVO target) {
        if (source == null || target == null) {
            return;
        }
        target.setPassedScore(ExamScoreUtil.toDisplayDouble(source.getPassedScore()));
        target.setGrossScore(ExamScoreUtil.toDisplayDouble(source.getGrossScore()));
        target.setRadioScore(ExamScoreUtil.toDisplayDouble(source.getRadioScore()));
        target.setMultiScore(ExamScoreUtil.toDisplayDouble(source.getMultiScore()));
        target.setJudgeScore(ExamScoreUtil.toDisplayDouble(source.getJudgeScore()));
        target.setSaqScore(ExamScoreUtil.toDisplayDouble(source.getSaqScore()));
        target.setCompoundScore(ExamScoreUtil.toDisplayDouble(source.getCompoundScore()));
    }

    @Mapping(target = "passedScore", ignore = true)
    @Mapping(target = "grossScore", ignore = true)
    @Mapping(target = "radioScore", ignore = true)
    @Mapping(target = "multiScore", ignore = true)
    @Mapping(target = "judgeScore", ignore = true)
    @Mapping(target = "saqScore", ignore = true)
    @Mapping(target = "compoundScore", ignore = true)
    ExamGradeListVO entityToExamGradeListVO(Exam exam);

    @AfterMapping
    default void fillExamGradeListDisplayScores(Exam source, @MappingTarget ExamGradeListVO target) {
        if (source == null || target == null) {
            return;
        }
        target.setPassedScore(ExamScoreUtil.toDisplayDouble(source.getPassedScore()));
        target.setGrossScore(ExamScoreUtil.toDisplayDouble(source.getGrossScore()));
        target.setRadioScore(ExamScoreUtil.toDisplayDouble(source.getRadioScore()));
        target.setMultiScore(ExamScoreUtil.toDisplayDouble(source.getMultiScore()));
        target.setJudgeScore(ExamScoreUtil.toDisplayDouble(source.getJudgeScore()));
        target.setSaqScore(ExamScoreUtil.toDisplayDouble(source.getSaqScore()));
        target.setCompoundScore(ExamScoreUtil.toDisplayDouble(source.getCompoundScore()));
    }

    /** 试卷下题目关联列表转阅卷/详情用扁平结构。 */
    List<ExamDetailRespVO> listEntityToExamDetailRespVO(List<ExamQuestion> examQuestion);

    /** 试卷题目关联转前端题目卡片 VO。 */
    ExamQuestionVO examQuestionEntityToVO(ExamQuestion examQuestion);

    /** 批量题目关联转 VO 列表。 */
    List<ExamQuestionVO> examQuestionListEntityToVO(List<ExamQuestion> examQuestion);

    /** 选项实体列表转选项 VO（展示题干选项文本等）。 */
    List<OptionVO> opListEntityToVO(List<Option> examQuestion);
}
