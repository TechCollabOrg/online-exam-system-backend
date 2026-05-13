package cn.org.alan.exam.model.vo.score;

import lombok.Data;

import java.io.Serializable;

/**
 * Mapper 中间行：某班在某场考试下一名学生的得分（用于计算班级名次）。
 */
@Data
public class PeerExamScoreRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer examId;
    private Integer userId;
    private Integer userScore;
}
