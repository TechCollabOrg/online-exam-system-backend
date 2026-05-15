package cn.org.alan.exam.model.vo.exam;

import lombok.Data;

import java.util.List;

/**
 * 考试答题卡题目列表 VO。
 */
@Data
public class ExamQuestionListVO {

    private Integer examDuration;

    private Long leftSeconds;

    private List<ExamQuestionVO> radioList;

    private List<ExamQuestionVO> multiList;

    private List<ExamQuestionVO> judgeList;

    private List<ExamQuestionVO> saqList;

    /** 复合题（题型 5）列表 */
    private List<ExamQuestionVO> compoundList;
}
