package cn.org.alan.exam.model.form.question;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.utils.QuestionImportValidators;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JSON 批量导入试题时单条记录。
 * <p>支持：普通题（quType 1～4）、复合题（quType 5 + {@link #subItems}）、
 * 材料组多行合并（{@link #stemGroupCode}，与 Excel「材料组编号」一致）。</p>
 */
@Data
public class QuestionJsonImportRow {

    private Integer quType;
    private String content;
    private String analysis;
    private String image;
    private List<JsonOption> options;
    /** 复合题（quType=5）小题列表；与 {@link #options} 互斥 */
    private List<QuestionSubItemForm> subItems;

    /** 同一编号的多行合并为一条复合题 */
    private String stemGroupCode;
    private String sharedStemContent;
    private String sharedStemImage;

    @Data
    public static class JsonOption {
        private String content;
        private Integer isRight;
        private String image;
        private String analysis;
    }

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

    public QuestionFrom toQuestionFrom(int index) {
        return toQuestionFrom(index, false);
    }

    public QuestionFrom toQuestionFrom(int index, boolean allowBlankContent) {
        String loc = String.format("JSON 第 %d 条", index);
        if (quType == null) {
            throw new ServiceRuntimeException(loc + "：缺少试题类型 quType（1单选 2多选 3判断 4简答 5复合题）");
        }
        if (quType == 5) {
            if (StringUtils.isNotBlank(stemGroupCode)) {
                throw new ServiceRuntimeException(loc + "：quType=5 的复合题不能同时填写 stemGroupCode，请用 subItems 或改用材料组多行导入");
            }
            return toCompoundQuestionFrom(loc);
        }
        if (!allowBlankContent && StringUtils.isBlank(content)) {
            throw new ServiceRuntimeException(loc + "：题干 content 不能为空");
        }

        List<Option> built = buildTopLevelOptions(loc, quType);
        String err = QuestionImportValidators.validateNormal(quType, built, loc);
        if (err != null) {
            throw new ServiceRuntimeException(err);
        }

        QuestionFrom qf = new QuestionFrom();
        qf.setContent(StringUtils.isBlank(content) ? null : content.trim());
        qf.setQuType(quType);
        qf.setAnalysis(StringUtils.isBlank(analysis) ? null : analysis.trim());
        qf.setImage(StringUtils.isBlank(image) ? null : image.trim());
        qf.setOptions(built);
        return qf;
    }

    private QuestionFrom toCompoundQuestionFrom(String loc) {
        if (subItems == null || subItems.isEmpty()) {
            throw new ServiceRuntimeException(loc + "：复合题 quType=5 须包含非空 subItems 数组");
        }
        if (options != null && !options.isEmpty()) {
            throw new ServiceRuntimeException(loc + "：复合题不要使用顶层 options，选项写在各 subItems[].options 中");
        }
        if (StringUtils.isBlank(content)) {
            throw new ServiceRuntimeException(loc + "：复合题共用材料 content 不能为空");
        }
        QuestionFrom qf = new QuestionFrom();
        qf.setQuType(5);
        qf.setContent(content.trim());
        qf.setAnalysis(StringUtils.isBlank(analysis) ? null : analysis.trim());
        qf.setImage(StringUtils.isBlank(image) ? null : image.trim());
        qf.setSubItems(normalizeSubItemSort(subItems));
        String compoundErr = QuestionImportValidators.validateCompound(qf);
        if (compoundErr != null) {
            throw new ServiceRuntimeException(loc + "：" + compoundErr);
        }
        return qf;
    }

    private List<Option> buildTopLevelOptions(String loc, int type) {
        List<Option> built = new ArrayList<>();
        if (options == null) {
            return built;
        }
        int oi = 0;
        for (JsonOption jo : options) {
            oi++;
            if (jo == null || StringUtils.isBlank(jo.getContent())) {
                continue;
            }
            Integer r = normalizeIsRight(jo.getIsRight());
            if (type != 4 && r == null) {
                throw new ServiceRuntimeException(String.format(
                        "%s：第 %d 个选项有 content 但未设置 isRight（0 或 1）", loc, oi));
            }
            Option o = new Option();
            o.setContent(jo.getContent().trim());
            o.setIsRight(type == 4 ? 1 : r);
            o.setImage(StringUtils.isBlank(jo.getImage()) ? null : jo.getImage().trim());
            o.setAnalysis(StringUtils.isBlank(jo.getAnalysis()) ? null : jo.getAnalysis().trim());
            built.add(o);
        }
        return built;
    }

    private static List<QuestionSubItemForm> normalizeSubItemSort(List<QuestionSubItemForm> items) {
        List<QuestionSubItemForm> out = new ArrayList<>();
        int sort = 0;
        for (QuestionSubItemForm item : items) {
            if (item == null) {
                continue;
            }
            QuestionSubItemForm copy = new QuestionSubItemForm();
            copy.setSort(item.getSort() != null ? item.getSort() : ++sort);
            copy.setContent(item.getContent());
            copy.setQuType(item.getQuType());
            copy.setOptions(item.getOptions());
            out.add(copy);
            if (item.getSort() == null) {
                sort = copy.getSort();
            }
        }
        return out;
    }

    /**
     * 从 JSON 对象解析一条导入记录（含 subItems、options）。
     */
    public static QuestionJsonImportRow fromJsonObject(JSONObject o, int index) {
        QuestionJsonImportRow row = new QuestionJsonImportRow();
        row.setQuType(o.getInteger("quType"));
        row.setContent(o.getString("content"));
        row.setAnalysis(o.getString("analysis"));
        row.setImage(o.getString("image"));
        row.setStemGroupCode(o.getString("stemGroupCode"));
        row.setSharedStemContent(o.getString("sharedStemContent"));
        row.setSharedStemImage(o.getString("sharedStemImage"));

        JSONArray subArr = o.getJSONArray("subItems");
        if (subArr != null && !subArr.isEmpty()) {
            row.setSubItems(parseSubItemsArray(subArr, index));
        }

        JSONArray opts = o.getJSONArray("options");
        if (opts != null) {
            List<JsonOption> list = new ArrayList<>();
            for (int j = 0; j < opts.size(); j++) {
                JSONObject jo = opts.getJSONObject(j);
                if (jo == null) {
                    continue;
                }
                JsonOption opt = new JsonOption();
                opt.setContent(jo.getString("content"));
                opt.setIsRight(normalizeIsRight(jo.get("isRight")));
                opt.setImage(jo.getString("image"));
                opt.setAnalysis(jo.getString("analysis"));
                list.add(opt);
            }
            row.setOptions(list);
        }
        return row;
    }

    public static List<QuestionSubItemForm> parseSubItemsArray(JSONArray subArr, int rowIndex) {
        String loc = String.format("JSON 第 %d 条", rowIndex);
        List<QuestionSubItemForm> subs = new ArrayList<>();
        for (int i = 0; i < subArr.size(); i++) {
            JSONObject jo = subArr.getJSONObject(i);
            if (jo == null) {
                throw new ServiceRuntimeException(loc + "：subItems[" + i + "] 不能为空");
            }
            QuestionSubItemForm sub = new QuestionSubItemForm();
            sub.setSort(jo.getInteger("sort"));
            sub.setContent(jo.getString("content"));
            sub.setQuType(jo.getInteger("quType"));
            JSONArray optArr = jo.getJSONArray("options");
            sub.setOptions(parseSubItemOptions(optArr, sub.getQuType(), loc, i + 1));
            subs.add(sub);
        }
        return subs;
    }

    private static List<QuestionSubItemOptionForm> parseSubItemOptions(
            JSONArray optArr, Integer quType, String loc, int subIndex) {
        List<QuestionSubItemOptionForm> out = new ArrayList<>();
        if (optArr == null) {
            return out;
        }
        int oi = 0;
        for (int j = 0; j < optArr.size(); j++) {
            JSONObject jo = optArr.getJSONObject(j);
            oi++;
            if (jo == null || StringUtils.isBlank(jo.getString("content"))) {
                continue;
            }
            Integer r = normalizeIsRight(jo.get("isRight"));
            if (quType != null && quType != 4 && r == null) {
                throw new ServiceRuntimeException(String.format(
                        "%s subItems[%d] 第 %d 个选项有 content 但未设置 isRight", loc, subIndex - 1, oi));
            }
            QuestionSubItemOptionForm f = new QuestionSubItemOptionForm();
            f.setContent(jo.getString("content").trim());
            f.setIsRight(quType != null && quType == 4 ? 1 : r);
            f.setImage(StringUtils.isBlank(jo.getString("image")) ? null : jo.getString("image").trim());
            f.setAnalysis(StringUtils.isBlank(jo.getString("analysis")) ? null : jo.getString("analysis").trim());
            out.add(f);
        }
        return out;
    }
}
