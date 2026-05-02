package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.CertificateUserMapper;
import cn.org.alan.exam.model.entity.CertificateUser;
import cn.org.alan.exam.service.ICertificateUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户获证记录 {@link cn.org.alan.exam.model.entity.CertificateUser}（证书编号、关联考试）的基础服务。
 *
 * @author WeiJin
 */
@Service
public class CertificateUserServiceImpl extends ServiceImpl<CertificateUserMapper, CertificateUser> implements ICertificateUserService {

}
