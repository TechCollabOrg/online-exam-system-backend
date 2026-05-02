package cn.org.alan.exam.controller;


import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.notice.NoticeForm;
import cn.org.alan.exam.model.vo.notice.NoticeVO;
import cn.org.alan.exam.service.INoticeService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公告发布与查看：教师/管理员维护，学生拉取可见列表。
 *
 * @author Alan
 */
@Api(tags = "公告管理相关接口")
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Resource
    private INoticeService noticeService;

    /** POST 发布公告（公开范围逻辑见 Service）。 */
    @ApiOperation("添加公告")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> addNotice(@Validated @RequestBody NoticeForm noticeForm) {
        return noticeService.addNotice(noticeForm);
    }

    /** DELETE 批量删除；路径 {@code ids} 多为逗号分隔。 */
    @ApiOperation("删除公告")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> deleteNotice(@PathVariable("ids") @NotBlank String ids) {
        return noticeService.deleteNotice(ids);
    }

    /** PUT 更新公告内容及可见班级关系。 */
    @ApiOperation("修改公告")
    @PutMapping("/{noticeId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> updateNotice(@PathVariable("noticeId") @NotBlank Integer noticeId, @Validated @RequestBody NoticeForm noticeForm) {
        return noticeService.updateNotice(noticeId, noticeForm);
    }

    /** GET 教师/管理员侧公告分页。 */
    @ApiOperation("教师分页查找")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<NoticeVO>> getNotice(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                             @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                             @RequestParam(value = "title", required = false) String title) {
        return noticeService.getNotice(pageNum, pageSize, title);
    }

    /** GET 学生可见公告分页。 */
    @ApiOperation("获取最新消息")
    @GetMapping("/new")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<IPage<NoticeVO>> getNewNotice(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return noticeService.getNewNotice(pageNum, pageSize);
    }
}
