package cn.org.alan.exam.utils;

import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.form.question.QuestionFrom;
import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.List;

/**
 * 试题 Excel/JSON 导入与手工录入共用的业务校验（与 {@code t_question}/{@code t_option} 结构一致）。
 */
public final class QuestionImportValidators {

    private QuestionImportValidators() {
    }

    /**
     * @return 错误说明；通过时返回 {@code null}
     */
    public static String validateCompound(QuestionFrom questionFrom) {
        if (StringUtils.isBlank(questionFrom.getContent())) {
            return "复合题须填写共用材料（content）";
        }
        List<QuestionSubItemForm> subItems = questionFrom.getSubItems();
        if (subItems == null || subItems.isEmpty()) {
            return "复合题至少需要一道小题（subItems）";
        }
        int idx = 0;
        for (QuestionSubItemForm sub : subItems) {
            idx++;
            if (sub == null) {
                return "第 " + idx + " 道小题数据无效";
            }
            if (sub.getQuType() == null || sub.getQuType() < 1 || sub.getQuType() > 4) {
                return "第 " + idx + " 道小题题型无效（quType 须为 1～4）";
            }
            if (!QuestionSubItemsUtil.hasMeaningfulStemContent(sub.getContent())) {
                return "第 " + idx + " 道小题题干不能为空（content 须有文字或 HTML 图片）";
            }
            List<QuestionSubItemOptionForm> opts = sub.getOptions();
            if (sub.getQuType() == 4) {
                if (opts == null || opts.isEmpty()) {
                    return "第 " + idx + " 道简答小题至少需要一个 options 元素（参考答案）";
                }
            } else if (opts == null || opts.size() < 2) {
                return "第 " + idx + " 道客观小题 options 不能少于两个";
            }
            if (sub.getQuType() != 4 && opts != null) {
                String optErr = validateObjectiveRights(sub.getQuType(), opts, "第 " + idx + " 道小题");
                if (optErr != null) {
                    return optErr;
                }
            }
        }
        return null;
    }

    /**
     * 普通题（1～4）选项校验。
     *
     * @return 错误说明；通过时返回 {@code null}
     */
    public static String validateNormal(int quType, List<Option> options, String location) {
        if (quType < 1 || quType > 4) {
            return location + "：试题类型 quType 无效（1单选 2多选 3判断 4简答 5复合题）";
        }
        if (quType == 4) {
            if (options == null || options.isEmpty()) {
                return location + "：简答题至少需要一个 options 元素（参考答案）";
            }
            return null;
        }
        if (options == null || options.size() < 2) {
            return location + "：客观题至少需要两个有内容的 options";
        }
        long rightCount = options.stream()
                .filter(o -> o != null && Integer.valueOf(1).equals(o.getIsRight()))
                .count();
        if (quType == 1 || quType == 3) {
            if (rightCount != 1) {
                return location + "：单选/判断题必须有且仅有一个 isRight=1 的选项";
            }
        } else if (quType == 2 && rightCount < 2) {
            return location + "：多选题至少两个 isRight=1 的选项";
        }
        return null;
    }

    private static String validateObjectiveRights(int quType, List<QuestionSubItemOptionForm> opts, String location) {
        long rightCount = 0;
        for (QuestionSubItemOptionForm o : opts) {
            if (o != null && Integer.valueOf(1).equals(o.getIsRight())) {
                rightCount++;
            }
        }
        if (quType == 1 || quType == 3) {
            if (rightCount != 1) {
                return location + "：单选/判断小题必须有且仅有一个 isRight=1 的选项";
            }
        } else if (quType == 2 && rightCount < 2) {
            return location + "：多选小题至少两个 isRight=1 的选项";
        }
        return null;
    }
}
