package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统角色；继承通用 CRUD，角色编码列表由 Mapper 支撑权限拼装。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IRoleService extends IService<Role> {

}
