package cn.org.alan.exam.model.vo.score;

import lombok.Data;


/**
 * 单场考试中某题的得分率、选项分布等统计分析。
 *
 * @author WeiJin
 * @since 2024/4/22
 */

@Data
public class QuestionAnalyseVO {
    // 正确数量
    private Integer rightCount;
    // 总题数
    private Integer totalCount;
    private Double accuracy;

}
