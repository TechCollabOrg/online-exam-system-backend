package cn.org.alan.exam.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgeTreeNodeVO;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgeTreeVO;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从大模型回复中解析题库知识树 JSON。
 */
public final class RepoKnowledgeTreeParser {

    private static final Pattern JSON_FENCE = Pattern.compile(
            "```json\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private RepoKnowledgeTreeParser() {
    }

    public static RepoKnowledgeTreeVO parse(String modelResponse) {
        if (StringUtils.isBlank(modelResponse)) {
            return null;
        }
        String jsonText = extractJsonText(modelResponse.trim());
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        JSONObject root = JSONUtil.parseObj(jsonText);
        RepoKnowledgeTreeVO vo = new RepoKnowledgeTreeVO();
        vo.setRootName(root.getStr("rootName", "知识树"));
        JSONArray nodes = root.getJSONArray("nodes");
        if (nodes != null) {
            vo.setNodes(parseNodes(nodes));
        }
        normalizeCounts(vo.getNodes());
        return vo;
    }

    private static List<RepoKnowledgeTreeNodeVO> parseNodes(JSONArray arr) {
        List<RepoKnowledgeTreeNodeVO> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            if (item == null) {
                continue;
            }
            RepoKnowledgeTreeNodeVO node = new RepoKnowledgeTreeNodeVO();
            node.setName(item.getStr("name", "未命名知识点"));
            node.setQuestionIds(parseQuestionIds(item.get("questionIds")));
            JSONArray children = item.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                node.setChildren(parseNodes(children));
            }
            list.add(node);
        }
        return list;
    }

    private static List<Integer> parseQuestionIds(Object raw) {
        List<Integer> ids = new ArrayList<>();
        if (raw == null) {
            return ids;
        }
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            for (int i = 0; i < arr.size(); i++) {
                Integer id = toInt(arr.get(i));
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids;
        }
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                Integer id = toInt(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private static Integer toInt(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void normalizeCounts(List<RepoKnowledgeTreeNodeVO> nodes) {
        if (nodes == null) {
            return;
        }
        for (RepoKnowledgeTreeNodeVO node : nodes) {
            if (node.getQuestionIds() == null) {
                node.setQuestionIds(new ArrayList<>());
            }
            node.setQuestionCount(node.getQuestionIds().size());
            normalizeCounts(node.getChildren());
        }
    }

    private static String extractJsonText(String response) {
        Matcher matcher = JSON_FENCE.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        return null;
    }
}
