package cn.org.alan.exam.model.vo.exam;

import cn.org.alan.exam.model.entity.ExamQuestion;
import lombok.Data;

import java.util.Calendar;
import java.util.List;

/**
 * 预览试卷：按题型分组的题目列表（单选/多选/判断/主观）。
 *
 * @author Alan
 * @since 2024/5/11
 */
@Data
public class ExamQuestionListVO {
    // 单选题列表
    private List<ExamQuestionVO> radioList;
    // 多选题列表
    private List<ExamQuestionVO> multiList;
    // 判断题列表
    private List<ExamQuestionVO> judgeList;
    // 简答题列表
    private List<ExamQuestionVO> saqList;
    private Integer examDuration;
    public Long leftSeconds;
}
