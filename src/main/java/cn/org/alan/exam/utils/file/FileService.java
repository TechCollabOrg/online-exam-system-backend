package cn.org.alan.exam.utils.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 对象存储上传抽象（具体实现如 MinIO、阿里云 OSS）：头像与题干图片等通过 {@link #upload(MultipartFile)} 返回公网或可访问 URL。
 *
 * @author 赵浩森
 */
public interface FileService {
    /**
     * 上传多端通用二进制文件（当前业务主要为图片），由实现类决定存储路径与访问域名。
     *
     * @param file 前端 multipart 文件
     * @return 访问 URL 或对象键
     */
    String upload(MultipartFile file) throws IOException;

    /**
     * 校验扩展名是否为常见图片类型（如 png/jpg/jpeg/bmp）。
     *
     * @param filename 原始文件名
     * @return 是否为允许的图片格式
     */
    boolean isImage(String filename);

    /**
     * 是否超过实现类配置的单文件大小上限（业务侧常与「50KB」提示文案对齐）。
     *
     * @param file 上传文件
     * @return true 表示超限
     */
    boolean isOverSize(MultipartFile file);
}