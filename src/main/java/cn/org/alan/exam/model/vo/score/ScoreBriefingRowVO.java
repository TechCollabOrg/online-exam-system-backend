package cn.org.alan.exam.model.vo.score;

import lombok.Data;

/**
 * 成绩简报用的单行考生成绩（含切屏次数）。
 */
@Data
public class ScoreBriefingRowVO {
    private String realName;
    private Integer userScore;
    private Integer cutScreenCount;
}
