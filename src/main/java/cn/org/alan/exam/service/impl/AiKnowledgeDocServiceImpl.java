package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.AiKnowledgeDocMapper;
import cn.org.alan.exam.model.entity.AiKnowledgeDoc;
import cn.org.alan.exam.model.form.ai.AiKnowledgeDocForm;
import cn.org.alan.exam.model.vo.ai.AiKnowledgeDocVO;
import cn.org.alan.exam.service.AiKnowledgeRagService;
import cn.org.alan.exam.service.IAiKnowledgeDocService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiKnowledgeDocServiceImpl extends ServiceImpl<AiKnowledgeDocMapper, AiKnowledgeDoc>
        implements IAiKnowledgeDocService {

    @Resource
    private AiKnowledgeRagService aiKnowledgeRagService;

    @PostConstruct
    public void initKnowledgeBase() {
        if (count() == 0) {
            try {
                importBuiltinIfEmpty();
            } catch (Exception e) {
                // 启动阶段不阻断应用
            }
        }
        aiKnowledgeRagService.reloadIndex();
    }

    @Override
    public Result<List<AiKnowledgeDocVO>> listAll(String keyword) {
        LambdaQueryWrapper<AiKnowledgeDoc> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> w.like(AiKnowledgeDoc::getTitle, keyword)
                    .or()
                    .like(AiKnowledgeDoc::getContent, keyword));
        }
        qw.orderByAsc(AiKnowledgeDoc::getSortOrder).orderByDesc(AiKnowledgeDoc::getId);
        List<AiKnowledgeDoc> rows = list(qw);
        return Result.success("ok", rows.stream().map(this::toVo).collect(Collectors.toList()));
    }

    @Override
    public Result<AiKnowledgeDocVO> getDetail(Integer id) {
        AiKnowledgeDoc row = getById(id);
        if (row == null) {
            return Result.failed("文档不存在");
        }
        return Result.success("ok", toVo(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> addDoc(AiKnowledgeDocForm form) {
        AiKnowledgeDoc row = fromForm(form);
        row.setCreateUserId(SecurityUtil.getUserId());
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateUserId(SecurityUtil.getUserId());
        row.setUpdateTime(LocalDateTime.now());
        save(row);
        aiKnowledgeRagService.reloadIndex();
        return Result.success("添加成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateDoc(Integer id, AiKnowledgeDocForm form) {
        AiKnowledgeDoc row = getById(id);
        if (row == null) {
            return Result.failed("文档不存在");
        }
        applyForm(row, form);
        row.setUpdateUserId(SecurityUtil.getUserId());
        row.setUpdateTime(LocalDateTime.now());
        updateById(row);
        aiKnowledgeRagService.reloadIndex();
        return Result.success("保存成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteDoc(Integer id) {
        if (!removeById(id)) {
            return Result.failed("文档不存在");
        }
        aiKnowledgeRagService.reloadIndex();
        return Result.success("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> importBuiltinIfEmpty() {
        if (count() > 0) {
            return Result.failed("知识库已有数据，无需重复导入");
        }
        int order = 0;
        int imported = 0;
        try {
            org.springframework.core.io.Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:ai-knowledge/*.md");
            for (org.springframework.core.io.Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                String text = readResource(resource);
                if (StringUtils.isBlank(text)) {
                    continue;
                }
                String filename = resource.getFilename() != null ? resource.getFilename() : "文档";
                AiKnowledgeDoc doc = new AiKnowledgeDoc();
                doc.setTitle(filename.replace(".md", ""));
                doc.setContent(text.trim());
                doc.setEnabled(1);
                doc.setSortOrder(order++);
                doc.setCreateUserId(null);
                doc.setCreateTime(LocalDateTime.now());
                doc.setUpdateTime(LocalDateTime.now());
                save(doc);
                imported++;
            }
        } catch (Exception e) {
            throw new ServiceRuntimeException("导入内置文档失败：" + e.getMessage());
        }
        if (imported == 0) {
            return Result.failed("未找到可导入的内置文档");
        }
        aiKnowledgeRagService.reloadIndex();
        return Result.success("已导入 " + imported + " 篇内置文档");
    }

    private AiKnowledgeDoc fromForm(AiKnowledgeDocForm form) {
        AiKnowledgeDoc row = new AiKnowledgeDoc();
        applyForm(row, form);
        return row;
    }

    private void applyForm(AiKnowledgeDoc row, AiKnowledgeDocForm form) {
        row.setTitle(form.getTitle().trim());
        row.setContent(form.getContent());
        row.setEnabled(Boolean.FALSE.equals(form.getEnabled()) ? 0 : 1);
        row.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
    }

    private AiKnowledgeDocVO toVo(AiKnowledgeDoc row) {
        AiKnowledgeDocVO vo = new AiKnowledgeDocVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setContent(row.getContent());
        vo.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        vo.setSortOrder(row.getSortOrder());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }

    private String readResource(org.springframework.core.io.Resource resource) throws Exception {
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
}
