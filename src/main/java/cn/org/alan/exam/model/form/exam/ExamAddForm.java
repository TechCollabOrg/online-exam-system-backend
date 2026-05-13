package cn.org.alan.exam.model.form.exam;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 新建考试：基本信息、抽题策略、开放时间与班级范围等。
 *
 * @author Alan
 * @since 2024/4/5
 */
@Data
public class ExamAddForm {
    // 考试标题
    @NotBlank(message = "考试标题不能为空")
    @Size(min = 3, max = 20, message = "请输入3-20个字符的考试标题")
    private String title;

    // 考试时长
    @NotNull(message = "请设置考试时间,单位m")
    @Min(value=0,message = "请设置大于0的考试时长")
    private Integer examDuration;

    // 最大切屏次数
    private Integer maxCount;

    // 及格分
    @Min(value=0,message = "及格分数必须大于0")
    @NotNull(message = "及格分不能为空")
    private Integer passedScore;

    // 开始时间
    @DateTimeFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime startTime;

    // 结束时间
    // @Future(message = "结束时间必须是一个必须是一个将来的日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime endTime;
    // 考试班级
    @NotBlank(message = "班级不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "班级参数错误，请将传输格式改为 1,2,3,4...且至少包含一个班级ID")
    private String gradeIds;

    // 题库ID（随机抽题必填，支持 1 或 1,2,3）；自己选题时由服务端根据所选试题推导，可留空
    @Pattern(regexp = "^$|^\\d+(,\\d+)*$|^\\d+$", message = "题库参数错误，请使用 1 或 1,2,3 格式")
    private String repoId;

    // 证书id（支持逗号分隔，服务端仅取第一个）
    @Pattern(regexp = "^$|^\\d+(,\\d+)*$|^\\d+$", message = "证书参数错误，请使用空字符串、1 或 1,2,3 格式")
    private String certificateId;

    // 单选题数量
    @NotBlank(message = "单选题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "单选题数量格式错误，请使用 0 或 0,1,2")
    private String radioCount;

    // 单选题分数
    @NotBlank(message = "单选题分数不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "单选题分数格式错误，请使用 0 或 1,1,1")
    private String radioScore;

    // 多选题数量
    @NotBlank(message = "多选题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "多选题数量格式错误，请使用 0 或 0,1,2")
    private String multiCount;

    // 多选题分数
    @NotBlank(message = "多选题分数不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "多选题分数格式错误，请使用 0 或 1,1,1")
    private String multiScore;

    // 判断题数量
    @NotBlank(message = "判断题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "判断题数量格式错误，请使用 0 或 0,1,2")
    private String judgeCount;

    // 判断题分数
    @NotBlank(message = "判断题分数不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "判断题分数格式错误，请使用 0 或 1,1,1")
    private String judgeScore;

    // 简答题数量
    @NotBlank(message = "简答题数量不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "简答题数量格式错误，请使用 0 或 0,1,2")
    private String saqCount;

    // 简答题分数
    @NotBlank(message = "简答题分数不能为空")
    @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$", message = "简答题分数格式错误，请使用 0 或 1,1,1")
    private String saqScore;

    // 简答题分数
    @NotBlank(message = "添加试题类型不能为空")
    private String addQuype;
    // 简答题分数
    private String quIds;
}
