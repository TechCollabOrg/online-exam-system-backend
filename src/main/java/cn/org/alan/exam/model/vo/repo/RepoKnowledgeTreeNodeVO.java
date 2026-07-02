package cn.org.alan.exam.model.vo.repo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 题库知识树节点。
 */
@Data
@ApiModel("题库知识树节点")
public class RepoKnowledgeTreeNodeVO {

    @ApiModelProperty("知识点名称")
    private String name;

    @ApiModelProperty("关联题目 ID 列表")
    private List<Integer> questionIds = new ArrayList<>();

    @ApiModelProperty("关联题目数量")
    private Integer questionCount;

    @ApiModelProperty("子节点")
    private List<RepoKnowledgeTreeNodeVO> children = new ArrayList<>();
}
