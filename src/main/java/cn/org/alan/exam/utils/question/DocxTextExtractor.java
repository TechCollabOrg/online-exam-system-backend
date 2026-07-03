package cn.org.alan.exam.utils.question;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;

/**
 * 从 .docx 提取纯文本，供 AI 试题转换使用。
 */
public final class DocxTextExtractor {

    private DocxTextExtractor() {
    }

    public static String extract(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (text == null || text.trim().isEmpty()) {
                throw new ServiceRuntimeException("Word 文档中未提取到有效文本");
            }
            return text.trim();
        } catch (ServiceRuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new ServiceRuntimeException("解析 Word 文档失败：" + e.getMessage());
        }
    }
}
