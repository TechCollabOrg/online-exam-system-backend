package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.service.IAiQuestionImportService;
import cn.org.alan.exam.service.IQuestionService;
import cn.org.alan.exam.utils.agent.AIChatRouter;
import cn.org.alan.exam.utils.question.AiJsonResponseExtractor;
import cn.org.alan.exam.utils.question.DocxTextExtractor;
import cn.org.alan.exam.utils.question.QuestionImportSpecLoader;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 将 docx/md 文档交给 AI 转为导入 JSON，再复用现有 JSON 导入逻辑入库。
 */
@Service
public class AiQuestionImportServiceImpl implements IAiQuestionImportService {

    private static final int MAX_DOC_CHARS = 48000;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    @Resource
    private AIChatRouter aiChatRouter;

    @Resource
    private IQuestionService questionService;

    @Override
    public Result<String> importFromDocument(Integer repoId, MultipartFile file) {
        if (repoId == null) {
            return Result.failed("请指定题库");
        }
        if (file == null || file.isEmpty()) {
            return Result.failed("请上传 Word 或 Markdown 文件");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            return Result.failed("文件不能超过 10MB");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String lower = filename.toLowerCase(Locale.ROOT);
        try {
            String documentText = extractDocumentText(file, lower);
            if (documentText.length() > MAX_DOC_CHARS) {
                return Result.failed("文档内容过长（超过 " + MAX_DOC_CHARS + " 字），请拆分后分批导入");
            }
            String systemPrompt = QuestionImportSpecLoader.buildAiSystemPrompt();
            String userMessage = "请将以下试题文档转换为符合规范的 JSON（根节点使用 {\"questions\":[...]}）：\n\n"
                    + documentText;
            String aiRaw = aiChatRouter.getQuestionImportResponse(systemPrompt, userMessage);
            String jsonText = AiJsonResponseExtractor.extract(aiRaw);
            return questionService.importQuestionsFromJson(repoId, jsonText.getBytes(StandardCharsets.UTF_8));
        } catch (ServiceRuntimeException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            return Result.failed("AI 导入失败：" + e.getMessage());
        }
    }

    private static String extractDocumentText(MultipartFile file, String lowerName) throws IOException {
        if (lowerName.endsWith(".docx")) {
            return DocxTextExtractor.extract(file.getInputStream());
        }
        if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            if (StringUtils.isBlank(text)) {
                throw new ServiceRuntimeException("Markdown 文件内容为空");
            }
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            return text;
        }
        throw new ServiceRuntimeException("AI 导入仅支持 .docx、.md、.markdown 文件");
    }
}
