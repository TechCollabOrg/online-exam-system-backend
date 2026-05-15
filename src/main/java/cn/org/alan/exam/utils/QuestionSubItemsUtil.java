package cn.org.alan.exam.utils;

import cn.org.alan.exam.model.form.question.QuestionSubItemForm;
import cn.org.alan.exam.model.form.question.QuestionSubItemOptionForm;
import cn.org.alan.exam.model.vo.exam.OptionVO;
import cn.org.alan.exam.model.vo.question.QuestionSubItemVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 复合题 {@code sub_items} JSON 与表单/VO 互转。
 */
public final class QuestionSubItemsUtil {

    private QuestionSubItemsUtil() {
    }

    /**
     * 小题题干是否有实质内容：纯文本、内嵌图片或非空 HTML（排除 Quill 空段落）。
     */
    public static boolean hasMeaningfulStemContent(String html) {
        if (StringUtils.isBlank(html)) {
            return false;
        }
        String s = html.trim();
        if (s.matches("(?is).*<img\\s.*")) {
            return true;
        }
        String text = s.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("\u200b", "")
                .replaceAll("\\s+", " ")
                .trim();
        return !text.isEmpty();
    }

    public static String toJson(List<QuestionSubItemForm> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<QuestionSubItemForm> normalized = new ArrayList<>();
        int sort = 0;
        for (QuestionSubItemForm item : items) {
            if (item == null) {
                continue;
            }
            QuestionSubItemForm copy = new QuestionSubItemForm();
            copy.setSort(++sort);
            copy.setContent(item.getContent());
            copy.setQuType(item.getQuType());
            copy.setOptions(item.getOptions() == null ? Collections.emptyList() : item.getOptions());
            normalized.add(copy);
        }
        return normalized.isEmpty() ? null : JSON.toJSONString(normalized);
    }

    public static List<QuestionSubItemForm> parseForms(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, QuestionSubItemForm.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<QuestionSubItemVO> parseToVoList(String json) {
        List<QuestionSubItemForm> forms = parseForms(json);
        List<QuestionSubItemVO> out = new ArrayList<>();
        for (QuestionSubItemForm form : forms) {
            QuestionSubItemVO vo = new QuestionSubItemVO();
            vo.setSort(form.getSort());
            vo.setContent(form.getContent());
            vo.setQuType(form.getQuType());
            vo.setOptions(toOptionVoList(form.getOptions()));
            out.add(vo);
        }
        return out;
    }

    private static List<OptionVO> toOptionVoList(List<QuestionSubItemOptionForm> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptionVO> vos = new ArrayList<>();
        int sort = 0;
        for (QuestionSubItemOptionForm opt : options) {
            if (opt == null) {
                continue;
            }
            OptionVO vo = new OptionVO();
            vo.setId(sort);
            vo.setSort(sort++);
            vo.setContent(opt.getContent());
            vo.setImage(opt.getImage());
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 解析复合题作答 JSON：key 为小题下标（字符串），value 为作答内容。
     */
    public static Map<String, Object> parseStudentAnswers(String raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (StringUtils.isBlank(raw)) {
            return out;
        }
        String t = raw.trim();
        if (!t.startsWith("{")) {
            return out;
        }
        try {
            JSONObject obj = JSON.parseObject(t);
            for (String key : obj.keySet()) {
                out.put(key, obj.get(key));
            }
        } catch (Exception ignored) {
            // ignore malformed payload
        }
        return out;
    }

    public static void applyCompoundStudentAnswers(List<QuestionSubItemVO> subItems, String savedContent) {
        if (subItems == null || subItems.isEmpty()) {
            return;
        }
        Map<String, Object> answers = parseStudentAnswers(savedContent);
        for (int i = 0; i < subItems.size(); i++) {
            QuestionSubItemVO item = subItems.get(i);
            Object ans = answers.get(String.valueOf(i));
            if (ans == null) {
                continue;
            }
            if (item.getQuType() != null && item.getQuType() == 4) {
                if (ans instanceof JSONArray) {
                    item.setStudentFill(((JSONArray) ans).toJSONString());
                } else {
                    item.setStudentFill(String.valueOf(ans));
                }
            } else if (item.getQuType() != null && item.getQuType() == 2) {
                if (ans instanceof JSONArray) {
                    item.setStudentAnswer(((JSONArray) ans).toJSONString());
                } else {
                    item.setStudentAnswer(String.valueOf(ans));
                }
            } else {
                item.setStudentAnswer(String.valueOf(ans));
            }
        }
    }

    public static List<String> parseSaqSlots(Object raw, int slotCount) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) {
            out.add("");
        }
        if (raw == null) {
            return out;
        }
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            for (int i = 0; i < slotCount && i < arr.size(); i++) {
                Object o = arr.get(i);
                out.set(i, o == null ? "" : String.valueOf(o));
            }
            return out;
        }
        String s = String.valueOf(raw).trim();
        if (s.startsWith("[")) {
            try {
                JSONArray arr = JSON.parseArray(s);
                for (int i = 0; i < slotCount && i < arr.size(); i++) {
                    Object o = arr.get(i);
                    out.set(i, o == null ? "" : String.valueOf(o));
                }
                return out;
            } catch (Exception ignored) {
                out.set(0, s);
                return out;
            }
        }
        out.set(0, s);
        return out;
    }
}
