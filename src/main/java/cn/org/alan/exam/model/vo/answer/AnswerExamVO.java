package cn.org.alan.exam.model.vo.answer;

import lombok.Data;

/**
 * 教师待阅卷列表中的考试项：考试 id、标题、是否含待阅主观题等。
 *
 * @author Alan
 * @since 2024/4/15
 */
@Data
public class AnswerExamVO {
    // 试卷ID
    private Integer examId;
    // 试卷标题
    private String examTitle;
    // 是否需要阅卷
    private Integer neededMark;
    // 班级总人数
    private Integer classSize;

    private Integer numberOfApplicants;
    // 已阅人数
    private Integer correctedPaper;
}
