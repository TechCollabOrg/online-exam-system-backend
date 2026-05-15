package cn.org.alan.exam.model.vo.exam;

import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import lombok.Data;

import java.util.List;

/**
 * 考试中单题详情 VO。
 */
@Data
public class ExamQuDetailVO {

    private Integer sort;

    private String image;

    private String content;

    private Integer quType;

    private List<OptionVO> answerList;

    /** 复合题（题型 5）下的小题列表 */
    private List<QuestionSubItemVO> subItemList;
}
