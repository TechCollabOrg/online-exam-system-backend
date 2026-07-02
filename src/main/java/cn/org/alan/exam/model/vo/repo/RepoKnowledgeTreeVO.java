package cn.org.alan.exam.model.vo.repo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 题库知识树视图。
 */
@Data
@ApiModel("题库知识树")
public class RepoKnowledgeTreeVO {

    @ApiModelProperty("题库 ID")
    private Integer repoId;

    @ApiModelProperty("题库名称")
    private String repoTitle;

    @ApiModelProperty("根节点名称（通常为学科或题库主题）")
    private String rootName;

    @ApiModelProperty("知识树节点")
    private List<RepoKnowledgeTreeNodeVO> nodes = new ArrayList<>();

    @ApiModelProperty("参与分析的题目总数")
    private Integer totalQuestions;

    @ApiModelProperty("生成时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;

    @ApiModelProperty("是否已生成")
    private Boolean generated;
}
