package cn.org.alan.exam.model.form.question;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.utils.excel.ExcelImport;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入试题时单行映射字段。
 * <p>支持「材料题 / 多小问」：同一 {@link #stemGroupCode} 的多行合并为一条复合题（题型 5），
 * 首行 {@link #sharedStemContent} 为共用材料，各行 {@link #content} 转为小题。</p>
 *
 * @author WeiJin
 * @since 2024/4/8
 */
@Data
public class QuestionExcelFrom {
    @ExcelImport(value = "试题类型", required = true)
    private Integer quType;
    /** 普通题必填；材料组内子题可为空（共用材料在「共用材料题干」） */
    @ExcelImport(value = "题干", unique = true)
    private String content;
    @ExcelImport(value = "解析")
    private String analysis;

    @ExcelImport(value = "题干图片")
    private String image;

    @ExcelImport(value = "选项一内容")
    private String option1;
    @ExcelImport(value = "选项一是否正确")
    private Integer righted1;
    @ExcelImport(value = "选项一图片")
    private String image1;

    @ExcelImport(value = "选项二内容")
    private String option2;
    @ExcelImport(value = "选项二是否正确")
    private Integer righted2;
    @ExcelImport(value = "选项二图片")
    private String image2;

    @ExcelImport(value = "选项三内容")
    private String option3;
    @ExcelImport(value = "选项三是否正确")
    private Integer righted3;
    @ExcelImport(value = "选项三图片")
    private String image3;

    @ExcelImport(value = "选项四内容")
    private String option4;
    @ExcelImport(value = "选项四是否正确")
    private Integer righted4;
    @ExcelImport(value = "选项四图片")
    private String image4;

    @ExcelImport(value = "选项五内容")
    private String option5;
    @ExcelImport(value = "选项五是否正确")
    private Integer righted5;
    @ExcelImport(value = "选项五图片")
    private String image5;

    @ExcelImport(value = "选项六内容")
    private String option6;
    @ExcelImport(value = "选项六是否正确")
    private Integer righted6;
    @ExcelImport(value = "选项六图片")
    private String image6;

    /** 同一编号的多行合并为一条复合题；首行须填「共用材料题干」 */
    @ExcelImport(value = "材料组编号")
    private String stemGroupCode;
    /** 仅本组第一行必填：共用材料（文字），上图用「共用材料题干图片」 */
    @ExcelImport(value = "共用材料题干")
    private String sharedStemContent;
    @ExcelImport(value = "共用材料题干图片")
    private String sharedStemImage;

    /**
     * 材料组编号去空白；空串视为未分组。
     */
    public static String normalizeStemGroupCode(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 将当前 Excel 行转为 {@link QuestionFrom}（材料组由导入逻辑合并为复合题）。
     */
    public QuestionFrom toQuestionFrom() {
        QuestionFrom questionFrom = new QuestionFrom();
        questionFrom.setContent(getContent());
        questionFrom.setQuType(getQuType());
        questionFrom.setAnalysis(getAnalysis());
        questionFrom.setImage(getImage());

        List<Option> options = new ArrayList<>();

        if (getQuType() != 4) {
            validateOptionRight(1, getOption1(), getRighted1());
            validateOptionRight(2, getOption2(), getRighted2());
            validateOptionRight(3, getOption3(), getRighted3());
            validateOptionRight(4, getOption4(), getRighted4());
            validateOptionRight(5, getOption5(), getRighted5());
            validateOptionRight(6, getOption6(), getRighted6());
        }

        addOptionIfPresent(options, getOption1(), getRighted1(), getImage1());
        addOptionIfPresent(options, getOption2(), getRighted2(), getImage2());
        addOptionIfPresent(options, getOption3(), getRighted3(), getImage3());
        addOptionIfPresent(options, getOption4(), getRighted4(), getImage4());
        addOptionIfPresent(options, getOption5(), getRighted5(), getImage5());
        addOptionIfPresent(options, getOption6(), getRighted6(), getImage6());

        questionFrom.setOptions(options);
        return questionFrom;
    }

    private void validateOptionRight(int idx, String optText, Integer righted) {
        if (optText != null && !optText.isEmpty() && righted == null) {
            throw new ServiceRuntimeException(String.format(
                    "导入错误 - 题干为「%s」的试题：选项%d内容存在但未设置是否正确，请检查 Excel「选项%d是否正确」列",
                    getContent(), idx, idx));
        }
    }

    private static void addOptionIfPresent(List<Option> options, String text, Integer righted, String img) {
        if (text != null && !text.isEmpty()) {
            Option option = new Option();
            option.setContent(text);
            option.setIsRight(righted);
            option.setImage(img);
            options.add(option);
        }
    }

    /**
     * 兼容旧调用：逐行 {@link #toQuestionFrom()}，不含材料组父题逻辑。
     */
    public static List<QuestionFrom> converterQuestionFrom(List<QuestionExcelFrom> questionExcelFroms) {
        List<QuestionFrom> list = new ArrayList<>(300);
        for (QuestionExcelFrom row : questionExcelFroms) {
            list.add(row.toQuestionFrom());
        }
        return list;
    }
}
