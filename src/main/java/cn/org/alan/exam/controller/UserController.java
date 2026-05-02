package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.common.group.UserGroup;
import cn.org.alan.exam.model.form.user.UserForm;
import cn.org.alan.exam.model.vo.user.UserVO;
import cn.org.alan.exam.service.IUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 登录后用户档案与后台用户运维：个人信息、密码、分页、导入、头像及学生加入班级。
 *
 * @author WeiJin
 */
@Api(tags = "用户管理相关接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private IUserService iUserService;

    /** GET 当前登录用户基本信息（脱敏）。 */
    @ApiOperation("获取用户个人信息")
    @GetMapping("/info")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<UserVO> info() {
        return iUserService.info();
    }


    /** POST 后台创建账号；校验分组 {@link UserGroup.CreateUserGroup}。 */
    @ApiOperation("创建用户")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> createUser(@Validated(UserGroup.CreateUserGroup.class) @RequestBody UserForm userForm) {
        return iUserService.createUser(userForm);
    }

    /** PUT 修改本人密码；成功后踢下线需重新登录。 */
    @ApiOperation("用户修改密码")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<String> updatePassword(@Validated(UserGroup.UpdatePasswordGroup.class) @RequestBody UserForm userForm) {
        return iUserService.updatePassword(userForm);
    }

    /** DELETE 批量删除；路径 {@code ids} 多为逗号分隔（与 Service 约定一致）。 */
    @ApiOperation("批量删除用户")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> deleteBatchByIds(@PathVariable("ids") String ids) {
        return iUserService.deleteBatchByIds(ids);
    }

    /** POST multipart 上传 Excel 批量导入用户。 */
    @ApiOperation("Excel导入用户数据")
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> importUsers(@RequestParam("file") MultipartFile file) {
        return iUserService.importUsers(file);
    }


    /** PUT 学生凭班级口令加入班级。 */
    @ApiOperation("用户加入班级")
    @PutMapping("/grade/join")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<String> joinGrade(@RequestParam("code") String code) {
        return iUserService.joinGrade(code);
    }

    /** GET 用户管理分页（教师限定所带班级学生）。 */
    @ApiOperation("分页获取用户信息")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<UserVO>> pagingUser(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                            @RequestParam(value = "gradeId", required = false) Integer gradeId,
                                            @RequestParam(value = "realName", required = false) String realName) {
        return iUserService.pagingUser(pageNum, pageSize, gradeId, realName);
    }

    /** PUT multipart 上传头像并更新用户表 URL。 */
    @ApiOperation("用户上传头像")
    @PutMapping("/uploadAvatar")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return iUserService.uploadAvatar(file);
    }
}
