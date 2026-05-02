package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.service.IFileService;
import cn.org.alan.exam.utils.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Objects;

/**
 * 头像等业务图片上传：委托 {@link cn.org.alan.exam.utils.file.FileService} 校验格式与大小后写入对象存储并返回 URL。
 *
 * @author Alan
 */
@Service
public class FileServiceImpl implements IFileService {
    @Resource
    private FileService fileService;

    /**
     * 校验为常见图片后缀且不超过存储适配器限制（当前提示 50KB），上传成功后返回可访问地址。
     */
    @SneakyThrows(IOException.class)
    @Override
    public Result<String> uploadImage(MultipartFile file) {
        if (!fileService.isImage(Objects.requireNonNull(file.getOriginalFilename()))) {
            throw new ServiceRuntimeException("上传头像到文件不是常用图片格式(png、jpg、jpeg、bmp)");
        }
        if (fileService.isOverSize(file)) {
            throw new ServiceRuntimeException("图片大小不能超过50KB");
        }
        String url = fileService.upload(file);
        if (StringUtils.isBlank(url)) {
            throw new ServiceRuntimeException("图片上传失败，url地址没有返回");
        }
        return Result.success("图片上传成功", url);
    }

}
