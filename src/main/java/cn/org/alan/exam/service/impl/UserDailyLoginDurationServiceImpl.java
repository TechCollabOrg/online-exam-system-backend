package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.UserDailyLoginDurationMapper;
import cn.org.alan.exam.model.entity.UserDailyLoginDuration;
import cn.org.alan.exam.service.IUserDailyLoginDurationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户每日在线/登录时长统计表的基础服务，仅使用框架生成方法。
 *
 * @author Alan
 */
@Service
public class UserDailyLoginDurationServiceImpl extends ServiceImpl<UserDailyLoginDurationMapper, UserDailyLoginDuration> implements IUserDailyLoginDurationService {
}
