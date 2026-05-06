package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.service.IFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 通用图片上传（如富文本、业务图片），与头像接口共用存储适配器。
 *
 * @author Alan
 */
@RestController
@Api(tags = "文件服务接口")
@RequestMapping("/api/upload")
public class FileController {
    private static final Pattern LOCAL_FILE =
            Pattern.compile("^[a-f0-9]{32}\\.(png|jpg|jpeg|bmp)$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private IFileService fileService;

    @Value("${online-exam.storage.local-dir:uploads/exam-files}")
    private String localUploadDir;

    /** POST multipart 上传图片并返回可访问 URL。 */
    @ApiOperation("上传图片")
    @PostMapping("/image")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return fileService.uploadImage(file);
    }

    /**
     * 本地存储模式（{@code online-exam.storage.type=local}）下，浏览器通过此地址加载已上传图片；匿名可访问以便 {@code img} 标签无需带 Token。
     */
    @ApiOperation("读取本地上传的图片文件")
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> readLocalFile(@PathVariable("fileName") String fileName) throws Exception {
        if (!LOCAL_FILE.matcher(fileName).matches()) {
            return ResponseEntity.notFound().build();
        }
        Path root = Paths.get(localUploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        String lower = fileName.toLowerCase();
        MediaType mediaType = MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".bmp")) {
            mediaType = MediaType.parseMediaType("image/bmp");
        }
        InputStream in = Files.newInputStream(file);
        InputStreamResource body = new InputStreamResource(in);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(body);
    }
}
