package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiKnowledgeDocForm;
import cn.org.alan.exam.model.vo.ai.AiKnowledgeDocVO;
import cn.org.alan.exam.service.IAiKnowledgeDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "AI 助手知识库")
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {

    @Resource
    private IAiKnowledgeDocService aiKnowledgeDocService;

    @ApiOperation("知识库文档列表（管理员）")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<List<AiKnowledgeDocVO>> list(@RequestParam(required = false) String keyword) {
        return aiKnowledgeDocService.listAll(keyword);
    }

    @ApiOperation("文档详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<AiKnowledgeDocVO> detail(@PathVariable Integer id) {
        return aiKnowledgeDocService.getDetail(id);
    }

    @ApiOperation("新增文档")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> add(@Validated @RequestBody AiKnowledgeDocForm form) {
        return aiKnowledgeDocService.addDoc(form);
    }

    @ApiOperation("更新文档")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> update(@PathVariable Integer id, @Validated @RequestBody AiKnowledgeDocForm form) {
        return aiKnowledgeDocService.updateDoc(id, form);
    }

    @ApiOperation("删除文档")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> delete(@PathVariable Integer id) {
        return aiKnowledgeDocService.deleteDoc(id);
    }

    @ApiOperation("从内置 Markdown 导入（仅当库为空时）")
    @PostMapping("/import-builtin")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> importBuiltin() {
        return aiKnowledgeDocService.importBuiltinIfEmpty();
    }
}
