package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.invite.InviteCodeForm;
import cn.org.alan.exam.model.vo.invite.InviteCodeVO;
import cn.org.alan.exam.service.IInviteCodeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 注册邀请码：仅管理员可生成与管理。
 */
@Api(tags = "邀请码管理")
@RestController
@RequestMapping("/api/invite-codes")
public class InviteCodeController {

    @Resource
    private IInviteCodeService inviteCodeService;

    @ApiOperation("生成邀请码")
    @PostMapping
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<String> create(@Validated @RequestBody InviteCodeForm form) {
        return inviteCodeService.create(form);
    }

    @ApiOperation("邀请码分页列表")
    @GetMapping("/paging")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<IPage<InviteCodeVO>> paging(
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            @RequestParam(value = "status", required = false) Integer status) {
        return inviteCodeService.paging(pageNum, pageSize, roleId, status);
    }

    @ApiOperation("禁用邀请码")
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<String> disable(@PathVariable("id") Integer id) {
        return inviteCodeService.disable(id);
    }

    @ApiOperation("批量删除邀请码")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<String> deleteBatch(@PathVariable("ids") String ids) {
        return inviteCodeService.deleteBatch(ids);
    }
}
