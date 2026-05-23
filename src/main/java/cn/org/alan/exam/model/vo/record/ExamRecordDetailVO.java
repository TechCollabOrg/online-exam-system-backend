package cn.org.alan.exam.model.vo.record;

import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import lombok.Data;

import java.util.List;

/**
 * 考后试卷解析单题 VO。
 */
@Data
public class ExamRecordDetailVO {

    /** 题目 ID（t_question.id） */
    private Integer quId;

    private String image;

    private String title;

    private Integer quType;

    private String analyse;

    private List<Option> option;

    private String rightOption;

    private List<QuestionSubItemVO> subItemList;

    /** 用户作答（客观题为选项序号等，简答题为文本） */
    private String myOption;

    /** 是否正确：1 对 0 错 -1 未判（如简答） */
    private Integer isRight;

    /** 该题满分（试卷内配置） */
    private Integer totalScore;

    /** 该题得分；未阅卷完成时可能为 null */
    private Integer quScore;

    /** 得分说明：如「待批改」 */
    private String scoreLabel;

    /** 子题时的父题 ID */
    private Integer parentQuId;

    /** 共用题干正文 */
    private String stemContent;

    /** 共用题干图片 */
    private String stemImage;
}
