package cn.org.alan.exam.model.form.question;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.entity.Option;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JSON 批量导入试题时单条记录：字段语义与 Excel 模板一致，选项使用 {@link #options} 数组表达。
 * <p>根 JSON 可为题对象数组 {@code [...]}，或包装为 {@code {"questions":[...]}}。</p>
 *
 * @author WeiJin
 */
@Data
public class QuestionJsonImportRow {

    private Integer quType;
    private String content;
    private String analysis;
    private String image;

    private List<JsonOption> options;

    /** 同一编号的多行共用首行 {@link #sharedStemContent}，与 Excel「材料组编号」一致 */
    private String stemGroupCode;
    private String sharedStemContent;
    private String sharedStemImage;

    @Data
    public static class JsonOption {
        private String content;
        private Integer isRight;
        private String image;
    }

    /**
     * 将「是否正确」列规范为 0/1：支持数字、布尔、常见中英文（与 Excel 导入列等价写法）。
     */
    public static Integer normalizeIsRight(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            int v = ((Number) raw).intValue();
            return v == 0 || v == 1 ? v : null;
        }
        if (raw instanceof Boolean) {
            return ((Boolean) raw) ? 1 : 0;
        }
        if (raw instanceof String) {
            String val = ((String) raw).trim();
            if (val.isEmpty()) {
                return null;
            }
            if ("0".equals(val) || "1".equals(val)) {
                return Integer.valueOf(val);
            }
            String lower = val.toLowerCase(Locale.ROOT);
            switch (lower) {
                case "true":
                case "yes":
                case "y":
                case "是":
                case "正确":
                case "对":
                case "√":
                    return 1;
                case "false":
                case "no":
                case "n":
                case "否":
                case "错误":
                case "错":
                case "×":
                case "x":
                    return 0;
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * 转为 {@link QuestionFrom}（不含 {@code parentQuId}，由导入逻辑按材料组写入）。
     */
    public QuestionFrom toQuestionFrom(int index) {
        if (quType == null) {
            throw new ServiceRuntimeException(String.format("JSON 第 %d 条：缺少试题类型 quType（1单选 2多选 3判断 4简答）", index));
        }
        if (StringUtils.isBlank(content)) {
            throw new ServiceRuntimeException(String.format("JSON 第 %d 条：题干 content 不能为空", index));
        }

        List<Option> built = new ArrayList<>();
        if (options != null) {
            int oi = 0;
            for (JsonOption jo : options) {
                oi++;
                if (jo == null || StringUtils.isBlank(jo.getContent())) {
                    continue;
                }
                Integer r = normalizeIsRight(jo.getIsRight());
                if (quType != 4 && r == null) {
                    throw new ServiceRuntimeException(String.format(
                            "JSON 第 %d 条：第 %d 个选项有内容但未设置是否正确（isRight，填 0 或 1）",
                            index, oi));
                }
                Option o = new Option();
                o.setContent(jo.getContent().trim());
                o.setIsRight(quType == 4 ? 1 : r);
                o.setImage(StringUtils.isBlank(jo.getImage()) ? null : jo.getImage().trim());
                built.add(o);
            }
        }

        if (quType == 4) {
            if (built.isEmpty()) {
                throw new ServiceRuntimeException(String.format(
                        "JSON 第 %d 条：简答题至少需要一个 options 元素（参考答案文本）", index));
            }
        } else if (built.size() < 2) {
            throw new ServiceRuntimeException(String.format(
                    "JSON 第 %d 条：非简答题至少需要两个有内容的选项", index));
        }

        QuestionFrom qf = new QuestionFrom();
        qf.setContent(content.trim());
        qf.setQuType(quType);
        qf.setAnalysis(StringUtils.isBlank(analysis) ? null : analysis.trim());
        qf.setImage(StringUtils.isBlank(image) ? null : image.trim());
        qf.setOptions(built);
        return qf;
    }
}
