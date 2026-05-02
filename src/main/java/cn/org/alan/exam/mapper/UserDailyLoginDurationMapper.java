package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.UserDailyLoginDuration;
import cn.org.alan.exam.model.vo.stat.DailyVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户每日在线时长记录 Mapper：按用户汇总日历曲线、查询当日一条记录。
 *
 * @author Alan
 * @since 2024/5/28
 */
public interface UserDailyLoginDurationMapper extends BaseMapper<UserDailyLoginDuration> {

    /**
     * 获取每天在线时长日志
     *
     * @param userId 用户ID
     * @return 结果集
     */
    List<DailyVO> getDaily(Integer userId);

    /**
     * 获得当天记录
     *
     * @param userId 用户ID
     * @param date   日期
     * @return 结果
     */
    UserDailyLoginDuration getTodayRecord(Integer userId, LocalDate date);
}
