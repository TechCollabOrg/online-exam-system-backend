package cn.org.alan.exam.model.vo.userbook;

import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import lombok.Data;

import java.util.List;

/**
 * 错题本中单题详情（题干、选项、解析等完整展示）。
 *
 * @author Alan
 * @since 2024/4/25
 */
@Data
public class BookOneQuVO {
    private static final long serialVersionUID = 1L;

    /**
     * 图片
     */
    private String image;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 试题类型
     */
    private Integer quType;

    /**
     * 答案内容
     */
    private List<Option> answerList;

    private String stemContent;
    private String stemImage;
    private Integer parentQuId;

    /** 复合题小题列表（quType=5） */
    private List<QuestionSubItemVO> subItemList;
}
