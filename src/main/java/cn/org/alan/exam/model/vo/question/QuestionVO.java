package cn.org.alan.exam.model.vo.question;

import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 试题详情 VO。
 */
@Data
public class QuestionVO {

    private Integer id;

    private Integer quType;

    private String image;

    private String content;

    private String analysis;

    private Integer repoId;

    private String repoTitle;

    private LocalDateTime createTime;

    private List<Option> options;

    /** 复合题小题（题型 5） */
    private List<QuestionSubItemForm> subItems;

    /** 子题时的父题 ID（历史多行拆题方案） */
    private Integer parentQuId;

    /** 共用题干正文 */
    private String stemContent;

    /** 共用题干图片 */
    private String stemImage;
}
