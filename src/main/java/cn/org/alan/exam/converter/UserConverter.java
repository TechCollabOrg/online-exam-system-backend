package cn.org.alan.exam.converter;


import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.form.user.UserForm;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link UserForm} 与 {@link User} 的 MapStruct 映射，供用户新增、批量导入等场景注入使用。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel = "spring")
public interface UserConverter {

    /** 单条注册/编辑表单转持久化实体。 */
    User fromToEntity(UserForm userForm);

    /** 批量表单转实体列表（如 Excel 导入）。 */
    List<User> listFromToEntity(List<UserForm> list);

}
