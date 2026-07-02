package cn.org.alan.exam.utils;

import cn.org.alan.exam.model.vo.repo.RepoKnowledgePointOptionVO;
import cn.org.alan.exam.model.vo.repo.RepoKnowledgeTreeNodeVO;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 题库知识树：节点路径解析与下拉选项扁平化。
 */
public final class RepoKnowledgeTreeUtil {

    private RepoKnowledgeTreeUtil() {
    }

    public static List<RepoKnowledgePointOptionVO> flattenOptions(List<RepoKnowledgeTreeNodeVO> nodes) {
        List<RepoKnowledgePointOptionVO> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (int i = 0; i < nodes.size(); i++) {
            collectOptions(nodes.get(i), String.valueOf(i), new ArrayList<>(), result);
        }
        return result;
    }

    public static List<Integer> resolveQuestionIds(List<RepoKnowledgeTreeNodeVO> nodes, String path) {
        if (nodes == null || StringUtils.isBlank(path)) {
            return new ArrayList<>();
        }
        RepoKnowledgeTreeNodeVO node = findNode(nodes, path);
        if (node == null) {
            return new ArrayList<>();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        collectQuestionIds(node, ids);
        return new ArrayList<>(ids);
    }

    private static void collectOptions(RepoKnowledgeTreeNodeVO node, String path,
                                       List<String> ancestors, List<RepoKnowledgePointOptionVO> result) {
        if (node == null) {
            return;
        }
        List<String> names = new ArrayList<>(ancestors);
        if (StringUtils.isNotBlank(node.getName())) {
            names.add(node.getName());
        }
        RepoKnowledgePointOptionVO option = new RepoKnowledgePointOptionVO();
        option.setPath(path);
        option.setName(node.getName());
        option.setLabel(String.join(" / ", names));
        option.setQuestionCount(countSubtreeQuestions(node));
        result.add(option);

        List<RepoKnowledgeTreeNodeVO> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            collectOptions(children.get(i), path + "-" + i, names, result);
        }
    }

    private static RepoKnowledgeTreeNodeVO findNode(List<RepoKnowledgeTreeNodeVO> nodes, String path) {
        String[] parts = path.split("-");
        RepoKnowledgeTreeNodeVO current = null;
        List<RepoKnowledgeTreeNodeVO> level = nodes;
        for (String part : parts) {
            if (level == null || level.isEmpty()) {
                return null;
            }
            int index;
            try {
                index = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return null;
            }
            if (index < 0 || index >= level.size()) {
                return null;
            }
            current = level.get(index);
            level = current.getChildren();
        }
        return current;
    }

    private static void collectQuestionIds(RepoKnowledgeTreeNodeVO node, Set<Integer> ids) {
        if (node == null) {
            return;
        }
        if (node.getQuestionIds() != null) {
            ids.addAll(node.getQuestionIds());
        }
        if (node.getChildren() != null) {
            for (RepoKnowledgeTreeNodeVO child : node.getChildren()) {
                collectQuestionIds(child, ids);
            }
        }
    }

    private static int countSubtreeQuestions(RepoKnowledgeTreeNodeVO node) {
        Set<Integer> ids = new LinkedHashSet<>();
        collectQuestionIds(node, ids);
        return ids.size();
    }
}
