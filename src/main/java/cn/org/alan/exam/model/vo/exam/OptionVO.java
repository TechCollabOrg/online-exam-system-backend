package cn.org.alan.exam.model.vo.exam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 考试中展示的选项：序号、文本、是否选中（考生视角）。
 *
 * @author Alan
 * @since 2024/5/20
 */
@Data
public class OptionVO {
    /**
     * id   选项答案表
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 试题id
     */
    private Integer quId;


    /**
     * 图片地址   0错误 1正确
     */
    private String image;

    /**
     * 选项内容
     */
    @NotBlank(message = "选型内容(content)不能为空")
    private String content;

    private Boolean checkout;
    /**
     * 排序
     */
    private Integer sort;
}
