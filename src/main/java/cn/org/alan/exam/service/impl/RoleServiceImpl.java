package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.RoleMapper;
import cn.org.alan.exam.model.entity.Role;
import cn.org.alan.exam.service.IRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 系统角色字典 {@link cn.org.alan.exam.model.entity.Role} 的基础 CRUD 封装。
 *
 * @author WeiJin
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

}
