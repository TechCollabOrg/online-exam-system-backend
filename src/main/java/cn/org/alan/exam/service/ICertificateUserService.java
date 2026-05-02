package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.CertificateUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户持证记录；继承通用 CRUD，记录证书颁发与用户的关联。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface ICertificateUserService extends IService<CertificateUser> {

}
