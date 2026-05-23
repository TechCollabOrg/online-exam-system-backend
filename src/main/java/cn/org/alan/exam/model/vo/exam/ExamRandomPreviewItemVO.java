package cn.org.alan.exam.model.vo.exam;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 随机组卷预览中的单题信息（含默认分值）。
 */
@Data
public class ExamRandomPreviewItemVO {

    private Integer id;

    private Integer quType;

    private String image;

    private String content;

    private Integer repoId;

    private String repoTitle;

    private LocalDateTime createTime;

    /** 按题型默认分给出的建议分值，可在保存前由前端修改 */
    private Integer assignScore;
}
