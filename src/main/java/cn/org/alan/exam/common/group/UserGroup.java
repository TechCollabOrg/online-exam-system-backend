package cn.org.alan.exam.common.group;

/**
 * Bean Validation 分组：用户创建、修改密码、注册等共用表单类时的分场景校验。
 *
 * @author WeiJin
 * @since 2024/3/29
 */
public interface UserGroup {

    /** 管理端/批量创建用户时的字段校验分组。 */
    interface CreateUserGroup extends UserGroup {
    }

    /** 修改密码接口的字段校验分组。 */
    interface UpdatePasswordGroup extends UserGroup {
    }

    /** 用户自主注册时的字段校验分组。 */
    interface RegisterGroup extends UserGroup {
    }
}
