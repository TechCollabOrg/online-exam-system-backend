package cn.org.alan.exam.model.vo.score;

import lombok.Data;

/**
 * 成绩简报用的单行考生成绩（含切屏次数）。
 */
@Data
public class ScoreBriefingRowVO {
    private String realName;
    /** 用户得分（展示分） */
    private Double userScore;
    private Integer cutScreenCount;
}
