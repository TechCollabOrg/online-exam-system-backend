package cn.org.alan.exam.model.vo.record;

import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import lombok.Data;

import java.util.List;

/**
 * 刷题记录详情页中单题维度：题干、作答与对错。
 *
 * @author Alan
 * @since 2024/4/30
 */
@Data
public class ExerciseRecordDetailVO {
    // 题干
    private String title;

    // 选项
    private List<Option> option;

    /** 复合题小题列表（quType=5） */
    private List<QuestionSubItemVO> subItemList;

    // 自己的答案
    private String myOption;

    // 正确的答案
    private String rightOption;

    // 是否正确
    private Integer isRight;

    // 试题分析
    private String analyse;

    // 题干图片
    private String image;

    /** 题干音频 */
    private String audio;

    // 试题类型
    private Integer quType;

    /** 共用题干正文 */
    private String stemContent;
    /** 共用题干附图 */
    private String stemImage;
    /** 共用题干音频 */
    private String stemAudio;
    /** 父题 id */
    private Integer parentQuId;
}
