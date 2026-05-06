package cn.org.alan.exam.utils.file.impl;

import cn.org.alan.exam.utils.file.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * MinIO 文件上传。注意：{@link MultipartFile#getInputStream()}{@code .available()} 通常不能代表文件长度，
 * 必须使用 {@link MultipartFile#getSize()} 作为 {@link PutObjectOptions} 的 objectSize，否则上传会失败并返回空 URL。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "online-exam.storage.type", havingValue = "minio")
public class MinioUtil implements FileService {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accesskey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String accessKeySecret;
    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public String upload(MultipartFile file) throws IOException {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "文件名为空");
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0) {
            throw new IOException("文件名缺少扩展名");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + originalFilename.substring(dot);

        long size = file.getSize();
        if (size <= 0) {
            throw new IOException("空文件或无法读取大小，请重新选择图片");
        }

        try {
            MinioClient client = new MinioClient(endpoint, accessKey, accessKeySecret);
            try {
                if (!client.bucketExists(bucketName)) {
                    client.makeBucket(bucketName);
                    log.info("已自动创建 MinIO 桶: {}", bucketName);
                }
            } catch (Exception e) {
                log.warn("检查/创建 MinIO 桶失败（仍将尝试上传）: {}", e.getMessage());
            }

            // PutObjectOptions(对象总字节数, 分片大小；-1 表示分片大小由客户端按默认策略处理)
            PutObjectOptions options = new PutObjectOptions(size, -1);
            try (InputStream inputStream = file.getInputStream()) {
                client.putObject(bucketName, fileName, inputStream, options);
            }
            return buildObjectUrl(fileName);
        } catch (Exception e) {
            log.error("MinIO 上传失败 bucket={} object={}", bucketName, fileName, e);
            throw new IOException("MinIO 上传失败，请确认 MinIO 已启动且账号/桶配置正确: " + e.getMessage(), e);
        }
    }

    private String buildObjectUrl(String fileName) {
        String ep = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return ep + "/" + bucketName + "/" + fileName;
    }

    @Override
    public boolean isImage(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return false;
        }
        String lastName = filename.substring(dot + 1).toLowerCase();
        String[] lastnames = {"png", "jpg", "jpeg", "bmp"};
        return Arrays.asList(lastnames).contains(lastName);
    }

    @Override
    public boolean isOverSize(MultipartFile file) {
        return file.getSize() > 20 * 1024 * 1024;
    }
}
