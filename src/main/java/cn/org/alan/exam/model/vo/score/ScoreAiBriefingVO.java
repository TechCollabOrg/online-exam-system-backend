package cn.org.alan.exam.model.vo.score;

import lombok.Data;

/**
 * AI 生成的成绩分析简报。
 */
@Data
public class ScoreAiBriefingVO {
    private String briefing;
    private String examTitle;
    private String gradeName;
    private Integer attendCount;
}
