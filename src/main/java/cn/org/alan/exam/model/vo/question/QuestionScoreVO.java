package cn.org.alan.exam.model.vo.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 阅卷待打分列表中的题目项：分值、学生作答摘录等。
 *
 * @author 赵浩森
 * @since 2024/4/13
 */
@Data
public class QuestionScoreVO {

    /**
     * 题目ID
     */
    @JsonProperty("题目ID")
    private String questionId;

    /**
     * 题目内容
     */
    @JsonProperty("题目内容")
    private String questionContent;

    /**
     * 题目总分
     */
    @JsonProperty("题目总分")
    private Integer totalScore;

    /**
     * 标准答案（多选项已聚合）
     */
    @JsonProperty("标准答案")
    private String qusetionAnswer;

    /**
     * 题目解析（题库 analysis 字段，供模型对照判分）
     */
    @JsonProperty("题目解析")
    private String questionAnalysis;

    /**
     * 联网检索摘要（可选，由 Serper 等注入）
     */
    @JsonProperty("参考资料")
    private String referenceMaterial;

    /**
     * 待评分答案
     */
    @JsonProperty("待评分答案")
    private String userAnswer;

}