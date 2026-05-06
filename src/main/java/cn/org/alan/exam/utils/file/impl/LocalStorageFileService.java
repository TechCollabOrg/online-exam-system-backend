package cn.org.alan.exam.utils.file.impl;

import cn.org.alan.exam.utils.file.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * 本地磁盘图片存储：不依赖 MinIO/OSS。
 * 返回<strong>相对路径</strong>{@code /api/upload/files/{uuid}.ext}}，便于开发代理与部署域名一致；
 * 若需邮件等场景绝对地址，可另在配置层拼接对外域名。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "online-exam.storage.type", havingValue = "local")
public class LocalStorageFileService implements FileService {

    @Value("${online-exam.storage.local-dir:uploads/exam-files}")
    private String localDir;

    @Override
    public String upload(MultipartFile file) throws IOException {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "文件名为空");
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0) {
            throw new IOException("文件名缺少扩展名");
        }
        String ext = originalFilename.substring(dot).toLowerCase();
        if (!isImage("x" + ext)) {
            throw new IOException("不支持的图片扩展名");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path root = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("非法保存路径");
        }
        file.transferTo(target.toFile());
        log.debug("本地存储已写入: {}", target);

        return "/api/upload/files/" + fileName;
    }

    @Override
    public boolean isImage(String filename) {
        int d = filename.lastIndexOf('.');
        if (d < 0 || d == filename.length() - 1) {
            return false;
        }
        String lastName = filename.substring(d + 1).toLowerCase();
        return Arrays.asList("png", "jpg", "jpeg", "bmp").contains(lastName);
    }

    @Override
    public boolean isOverSize(MultipartFile file) {
        return file.getSize() > 20 * 1024 * 1024;
    }
}
