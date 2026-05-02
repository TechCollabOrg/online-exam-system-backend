package cn.org.alan.exam.model.vo.exercise;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 刷题答题卡上的单题摘要：是否已做过、题型等。
 *
 * @author WeiJin
 * @since 2024/4/29
 */
@Data
public class QuestionSheetVO {
    // 试题ID
    private Integer quId;
    // 试题类型
    private Integer quType;
    // 题库ID
    private Integer repoId;
    private Integer exercised;
    private Integer isRight;
}
