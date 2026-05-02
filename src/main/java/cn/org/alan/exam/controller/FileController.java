package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.service.IFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用图片上传（如富文本、业务图片），与头像接口共用存储适配器。
 *
 * @author Alan
 */
@RestController
@Api(tags = "文件服务接口")
@RequestMapping("/api/upload")
public class FileController {
    @Autowired
    private IFileService fileService;

    /** POST multipart 上传图片并返回可访问 URL。 */
    @ApiOperation("上传图片")
    @PostMapping("/image")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return fileService.uploadImage(file);
    }
}
