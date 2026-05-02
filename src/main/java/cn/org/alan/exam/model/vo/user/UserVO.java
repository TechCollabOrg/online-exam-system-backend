package cn.org.alan.exam.model.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理分页/详情：账号、角色、班级、头像等对外展示字段。
 *
 * @author WeiJin
 * @since 2024/3/31
 */
@Data
public class UserVO {

    private Integer id;
    // 用户名称
    private String userName;
    // 真实姓名
    private String realName;
    // 用户角色
    private Integer roleId;
    // 密码
    private String password;
    // 头像
    private String avatar;
    // 加入的班级名称
    private String gradeName;
    // 班级ID
    private Integer gradeId;
    // 班级创建人Id
    private Integer userId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private String status;

}
