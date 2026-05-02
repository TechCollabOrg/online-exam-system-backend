package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传：将图片等资源写入配置存储并返回可访问地址。
 *
 * @author Alan
 * @since 2025/3/21
 */
public interface IFileService {

    /**
     * 上传图片
     *
     * @param file 文件
     * @return 返回上传后的地址
     */
    Result<String> uploadImage(MultipartFile file);
}
