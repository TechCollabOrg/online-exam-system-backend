package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.UserDailyLoginDuration;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户每日在线时长；继承通用 CRUD，统计曲线由实现类与定时任务配合。
 *
 * @author Alan
 * @since 2024/5/28
 */
public interface IUserDailyLoginDurationService extends IService<UserDailyLoginDuration> {
}
