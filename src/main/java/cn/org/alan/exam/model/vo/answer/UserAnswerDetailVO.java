package cn.org.alan.exam.model.vo.answer;

import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import lombok.Data;

import java.util.List;

/**
 * 阅卷侧查看某考生在某题上的作答全文与附件标识。
 *
 * @author WeiJin
 * @since 2024/4/29
 */
@Data
public class UserAnswerDetailVO {
    // 试题ID
    private Integer quId;
    // 用户ID
    private Integer userId;
    // 试卷ID
    private Integer examId;
    // 试题标题
    private String quTitle;
    // 试题图片
    private String quImg;
    private String answer;
    private String refAnswer;
    private Integer correctScore;
    private String aiReason;
    private Integer totalScore;

    /** 试题类型：1单选 2多选 3判断 4简答 5复合题 */
    private Integer quType;

    /** 复合题小题列表（含考生作答，仅 quType=5 时有值） */
    private List<QuestionSubItemVO> subItemList;

}
