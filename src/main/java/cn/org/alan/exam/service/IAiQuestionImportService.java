package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 试题导入：将 docx/md 转为 JSON 并写入题库。
 */
public interface IAiQuestionImportService {

    /**
     * 上传 Word/Markdown 文档，经 AI 转为规范 JSON 后导入指定题库。
     *
     * @param repoId 题库 ID
     * @param file   .docx 或 .md/.markdown 文件
     */
    Result<String> importFromDocument(Integer repoId, MultipartFile file);
}
