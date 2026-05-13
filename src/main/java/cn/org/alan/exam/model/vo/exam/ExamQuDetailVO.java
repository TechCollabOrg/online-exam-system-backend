package cn.org.alan.exam.model.vo.exam;

import cn.org.alan.exam.model.entity.Option;
import lombok.Data;

import java.util.List;

/**
 * 进入考试后单题完整内容：题干、选项、已选状态、标记等。
 *
 * @author Alan
 * @since 2024/4/1
 */
@Data
public class ExamQuDetailVO {
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
     * 答案内容
     */
    private List<OptionVO> answerList;
    // 试题类型
    private Integer quType;
    /**
     * 排序
     */
    private Integer sort;

    /** 共用题干正文（来自 parent_qu_id 指向的父题），无材料题时为 null */
    private String stemContent;
    /** 共用题干附图（父题 image） */
    private String stemImage;
    /** 父题 id，与试题表 parent_qu_id 一致；无材料题时为 null */
    private Integer parentQuId;

}
