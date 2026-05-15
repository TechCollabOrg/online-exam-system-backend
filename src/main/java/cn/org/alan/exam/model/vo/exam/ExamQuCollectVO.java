package cn.org.alan.exam.model.vo.exam;

import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import lombok.Data;

import java.util.List;

/**
 * 交卷前题目汇总 VO。
 */
@Data
public class ExamQuCollectVO {

    private Integer id;

    private String title;

    private Integer quType;

    private List<Option> option;

    private List<QuestionSubItemVO> subItemList;

    private String myOption;

    private Integer isRight;
}
