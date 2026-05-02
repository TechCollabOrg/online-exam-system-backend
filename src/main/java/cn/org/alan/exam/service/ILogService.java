package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.Log;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 登录与操作日志：写入登录日志、管理员分页查询。
 *
 * @author Alan
 * @since 2025/4/4
 */
public interface ILogService {
    /**
     * 记录登录日志
     * @param log
     * @return
     */
    Log add(Log log);

    /**
     * 分页查询登录日志
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<Log> getPage(Integer pageNum, Integer pageSize);
}
