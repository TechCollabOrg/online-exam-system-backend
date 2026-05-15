package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目主表：类型、分值、所属题库、题干与解析等。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
@Data
@ApiModel("试题实体类")
@TableName("t_question")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("试题ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 1单选 2多选 3判断 4简答 5复合题（共用材料+多小题） */
    @ApiModelProperty("试题类型：1单选2多选3判断4简答5复合题")
    private Integer quType;

    @ApiModelProperty("试题图片")
    private String image;

    @ApiModelProperty("题干")
    private String content;

    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty("题目分析")
    private String analysis;

    @ApiModelProperty("题库ID")
    private Integer repoId;

    @ApiModelProperty("创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Integer userId;

    @TableLogic
    @ApiModelProperty("逻辑删除字段")
    private Integer isDeleted;

    /**
     * 复合题小题 JSON（题型 5）：共用材料在 content，各小题结构见 {@code QuestionSubItemForm}。
     */
    @ApiModelProperty("复合题小题JSON")
    private String subItems;

    /**
     * 子题关联的共用题干题目 ID（历史「多行拆题」方案）；与 {@link #subItems} JSON 可并存，未使用时为 null。
     */
    @TableField(exist = false)
    @ApiModelProperty("父题ID（子题时非空）")
    private Integer parentQuId;
}
