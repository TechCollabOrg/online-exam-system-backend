package cn.org.alan.exam.model.vo.question;

import cn.org.alan.exam.model.vo.exam.OptionVO;
import lombok.Data;

import java.util.List;

/**
 * 复合题小题（考试/详情展示）。
 */
@Data
public class QuestionSubItemVO {

    private Integer sort;

    private String content;

    private Integer quType;

    private List<OptionVO> options;

    /** 客观题：选中选项下标；多选为下标数组的 JSON 字符串 */
    private String studentAnswer;

    /** 简答题：单空文本或多空 JSON 数组字符串 */
    private String studentFill;
}
