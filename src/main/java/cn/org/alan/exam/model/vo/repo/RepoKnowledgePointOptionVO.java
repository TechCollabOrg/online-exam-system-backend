package cn.org.alan.exam.model.vo.repo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 知识树下拉选项（用于按知识点筛选题目）。
 */
@Data
@ApiModel("题库知识点选项")
public class RepoKnowledgePointOptionVO {

    @ApiModelProperty("节点路径，如 0、0-1、0-1-2")
    private String path;

    @ApiModelProperty("知识点名称")
    private String name;

    @ApiModelProperty("展示标签（含父级路径）")
    private String label;

    @ApiModelProperty("该节点及子节点关联题目总数")
    private Integer questionCount;
}
