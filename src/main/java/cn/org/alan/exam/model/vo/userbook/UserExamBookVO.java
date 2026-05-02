package cn.org.alan.exam.model.vo.userbook;

import cn.org.alan.exam.model.entity.Option;
import lombok.Data;

import java.util.List;

/**
 * 某场考试下错题列表中单题的题干与选项等展示数据。
 *
 * @author Alan
 * @since 2024/4/25
 */
@Data
public class UserExamBookVO {
    /**
     * 题干
     */
    private String content;
    /**
     * 选项
     */
    private List<Option> answerList;
    /**
     * 正确答案
     */
    private String rightAnswers;
    /**
     * 试题分析
     */
    private String analyse;
    /**
     * 所填答案
     */
    private String reply;

}
