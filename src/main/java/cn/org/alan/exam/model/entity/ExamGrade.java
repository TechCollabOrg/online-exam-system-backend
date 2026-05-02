package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 考试与参考班级的关联：哪些班级参加本场考试及扩展字段。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
@Data
@ApiModel("考试班级关联")
@TableName("t_exam_grade")
public class ExamGrade implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("考试与班级关系表ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("考试ID")
    private Integer examId;

    @ApiModelProperty("班级ID")
    private Integer gradeId;
}
