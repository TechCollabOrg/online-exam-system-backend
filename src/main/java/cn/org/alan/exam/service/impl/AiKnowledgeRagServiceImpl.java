package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.service.AiKnowledgeRagService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 classpath 下 Markdown 文档的轻量 RAG（关键词匹配），不连接题库数据库。
 */
@Slf4j
@Service
public class AiKnowledgeRagServiceImpl implements AiKnowledgeRagService {

    private static final int TOP_K = 4;
    private static final int MAX_CHUNK_CHARS = 500;

    /** 含此类内容的片段不进入知识库（防止误导入题库/阅卷样本） */
    private static final String[] FORBIDDEN_SNIPPETS = {
            "待评分答案", "标准答案", "题目ID", "t_question", "t_option",
            "评分结果", "扣分原因", "ImportQuestionTemplate", "JSON 示例",
            "示例输入", "示例输出", "qu_type="
    };

    private final List<KnowledgeChunk> chunks = new ArrayList<>();

    @PostConstruct
    public void loadKnowledgeBase() {
        chunks.clear();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:ai-knowledge/*.md");
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                String text = readResource(resource);
                if (StringUtils.isBlank(text)) {
                    continue;
                }
                splitIntoChunks(text, resource.getFilename()).forEach(chunk -> {
                    if (!isForbidden(chunk.text)) {
                        chunks.add(chunk);
                    }
                });
            }
            log.info("AI 助手知识库已加载 {} 个片段", chunks.size());
        } catch (Exception e) {
            log.warn("AI 助手知识库加载失败: {}", e.getMessage());
        }
    }

    @Override
    public String retrieveContext(String query) {
        if (chunks.isEmpty() || StringUtils.isBlank(query)) {
            return "";
        }
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return chunks.stream().limit(2).map(c -> c.text).collect(Collectors.joining("\n\n---\n\n"));
        }
        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            double score = scoreChunk(chunk.text, queryTokens, query);
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        if (scored.isEmpty()) {
            return chunks.stream().limit(2).map(c -> c.text).collect(Collectors.joining("\n\n---\n\n"));
        }
        return scored.stream()
                .limit(TOP_K)
                .map(s -> s.chunk.text)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private double scoreChunk(String text, Set<String> queryTokens, String rawQuery) {
        String lower = text.toLowerCase(Locale.ROOT);
        String rawLower = rawQuery.toLowerCase(Locale.ROOT);
        double score = 0;
        for (String token : queryTokens) {
            if (lower.contains(token)) {
                score += 1.0;
            }
        }
        if (lower.contains(rawLower) && rawLower.length() >= 2) {
            score += 2.0;
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String part : text.toLowerCase(Locale.ROOT).split("[\\s，。、；：？！,.;:!?\\n]+")) {
            if (part.length() >= 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private boolean isForbidden(String text) {
        if (StringUtils.isBlank(text)) {
            return true;
        }
        for (String forbidden : FORBIDDEN_SNIPPETS) {
            if (text.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }

    private List<KnowledgeChunk> splitIntoChunks(String markdown, String source) {
        List<KnowledgeChunk> result = new ArrayList<>();
        String[] sections = markdown.split("(?=\\n## )");
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= MAX_CHUNK_CHARS) {
                result.add(new KnowledgeChunk(source, trimmed));
            } else {
                for (int i = 0; i < trimmed.length(); i += MAX_CHUNK_CHARS) {
                    int end = Math.min(i + MAX_CHUNK_CHARS, trimmed.length());
                    result.add(new KnowledgeChunk(source, trimmed.substring(i, end)));
                }
            }
        }
        return result;
    }

    private String readResource(Resource resource) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static class KnowledgeChunk {
        final String source;
        final String text;

        KnowledgeChunk(String source, String text) {
            this.source = source;
            this.text = text;
        }
    }

    private static class ScoredChunk {
        final KnowledgeChunk chunk;
        final double score;

        ScoredChunk(KnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        double getScore() {
            return score;
        }
    }
}
