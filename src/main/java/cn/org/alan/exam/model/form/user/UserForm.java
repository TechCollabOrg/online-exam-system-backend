package cn.org.alan.exam.model.form.user;

import cn.org.alan.exam.common.group.UserGroup;
import cn.org.alan.exam.utils.excel.ExcelImport;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;


/**
 * 用户创建、导入或后台编辑时的表单字段集合。
 *
 * @author WeiJin
 * @since 2024/3/29
 */
@Data
public class UserForm {
    // 用户ID
    @NotNull(groups = {UserGroup.AdminUpdateUserGroup.class}, message = "用户ID不能为空")
    private Integer id;

    // 创建试卷
    private LocalDateTime createTime;

    // 用户吗
    @NotBlank(groups = {UserGroup.CreateUserGroup.class, UserGroup.RegisterGroup.class}, message = "用户名不能为空")
    //EasyExcel注解，映射关系
    @ExcelImport(value = "用户名*",unique = true,required = true)
    private String userName;

    // 密码
    @NotBlank(groups = UserGroup.RegisterGroup.class,message = "密码不能为空")
    private String password;

    // 真实姓名
    @NotBlank(groups = {UserGroup.CreateUserGroup.class, UserGroup.RegisterGroup.class}, message = "真实姓名不能为空")
    @ExcelImport(value = "真实姓名*")
    private String realName;

    // 角色ID：注册时 1学生 2教师 3管理员
    @NotNull(groups = UserGroup.RegisterGroup.class, message = "请选择注册身份")
    @Min(value = 1, groups = UserGroup.RegisterGroup.class, message = "注册身份无效")
    @Max(value = 3, groups = UserGroup.RegisterGroup.class, message = "注册身份无效")
    @ExcelImport(value = "角色")
    private Integer roleId;

    /** 教师/管理员注册必填；学生无需填写 */
    private String inviteCode;

    // 班级ID
    private Integer gradeId;

    /** 学生专业 */
    @ExcelImport(value = "专业")
    private String major;

    // 旧密码
    @NotBlank(groups = {UserGroup.UpdatePasswordGroup.class}, message = "原密码不能为空")
    private String originPassword;

    // 新密码
    @NotBlank(groups = {UserGroup.UpdatePasswordGroup.class}, message = "新密码不能为空")
    private String newPassword;

    // 校验密码
    @NotBlank(groups = {UserGroup.UpdatePasswordGroup.class, UserGroup.RegisterGroup.class}, message = "校验密码不能为空")
    private String checkedPassword;

    /** 注册前须通过 verifyCode；与 {@link #getCaptchaJson} 返回的 captchaId 一致 */
    private String captchaId;

}
