package cn.org.alan.exam.model.vo.exam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;

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
     * 选项内容（客观题为选项文字；简答题为各空参考答案 HTML）
     */
    private String content;

    private Boolean checkout;
    /**
     * 排序
     */
    private Integer sort;

    /**
     * 简答题多格作答时，该格学生已填内容（与 {@link #content} 题库参考答案区分；客观题不使用）。
     */
    private String studentFill;
}
