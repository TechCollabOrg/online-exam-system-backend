package cn.org.alan.exam.model.vo.record;

import cn.org.alan.exam.model.entity.Option;
import lombok.Data;

import java.util.List;

/**
 * 考试回顾详情中单题：选项、用户答案、标准答案与解析。
 *
 * @author Alan
 * @since 2024/4/30
 */
@Data
public class ExamRecordDetailVO {
    // 1、题干 2、选项 3、自己的答案 4、正确的答案 5、是否正确 6、试题分析
    /**
     * 题干
     */
    private String title;
    /**
     * 题干图片
     */
    private String image;
    /**
     * 选项
     */
    private List<Option> option;
    /**
     * 我的答案
     */
    private String myOption;
    /**
     * 正确答案
     */
    private String rightOption;
    /**
     * 是否正确
     */
    private Integer isRight;
    /**
     * 试题分析
     */
    private String analyse;
    /**
     * 试题类型
     */
    private Integer quType;

    /** 共用题干正文 */
    private String stemContent;
    /** 共用题干附图 */
    private String stemImage;
    /** 父题 id */
    private Integer parentQuId;

}
