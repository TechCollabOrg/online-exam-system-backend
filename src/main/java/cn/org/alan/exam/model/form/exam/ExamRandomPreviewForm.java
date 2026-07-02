package cn.org.alan.exam.model.form.exam;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 随机组卷预览：与创建考试随机抽题规则相同，仅用于预览抽题结果。
 */
@Data
public class ExamRandomPreviewForm {

    @NotBlank(message = "题库不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "题库参数错误，请使用 1 或 1,2,3 格式")
    private String repoId;

    @NotBlank(message = "单选题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "单选题数量格式错误")
    private String radioCount;

    @NotBlank(message = "单选题分数不能为空")
    @Pattern(regexp = "^(\\d+(\\.\\d{1,2})?)(,(\\d+(\\.\\d{1,2})?))*$|^(\\d+(\\.\\d{1,2})?)$", message = "单选题分数格式错误，请使用 0、1.5 或 1,1,1.5")
    private String radioScore;

    @NotBlank(message = "多选题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "多选题数量格式错误")
    private String multiCount;

    @NotBlank(message = "多选题分数不能为空")
    @Pattern(regexp = "^(\\d+(\\.\\d{1,2})?)(,(\\d+(\\.\\d{1,2})?))*$|^(\\d+(\\.\\d{1,2})?)$", message = "多选题分数格式错误，请使用 0、1.5 或 1,1,1.5")
    private String multiScore;

    @NotBlank(message = "判断题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "判断题数量格式错误")
    private String judgeCount;

    @NotBlank(message = "判断题分数不能为空")
    @Pattern(regexp = "^(\\d+(\\.\\d{1,2})?)(,(\\d+(\\.\\d{1,2})?))*$|^(\\d+(\\.\\d{1,2})?)$", message = "判断题分数格式错误，请使用 0、1.5 或 1,1,1.5")
    private String judgeScore;

    @NotBlank(message = "简答题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "简答题数量格式错误")
    private String saqCount;

    @NotBlank(message = "简答题分数不能为空")
    @Pattern(regexp = "^(\\d+(\\.\\d{1,2})?)(,(\\d+(\\.\\d{1,2})?))*$|^(\\d+(\\.\\d{1,2})?)$", message = "简答题分数格式错误，请使用 0、1.5 或 1,1,1.5")
    private String saqScore;

    @NotBlank(message = "复合题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "复合题数量格式错误")
    private String compoundCount;

    @NotBlank(message = "复合题分数不能为空")
    @Pattern(regexp = "^(\\d+(\\.\\d{1,2})?)(,(\\d+(\\.\\d{1,2})?))*$|^(\\d+(\\.\\d{1,2})?)$", message = "复合题分数格式错误，请使用 0、1.5 或 1,1,1.5")
    private String compoundScore;
}
