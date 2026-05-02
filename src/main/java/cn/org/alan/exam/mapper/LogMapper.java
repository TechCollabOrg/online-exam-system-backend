package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.Log;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志表 Mapper，使用 BaseMapper 通用 CRUD。
 *
 * @author Alan
 * @since 2025/4/4
 */
@Mapper
public interface LogMapper extends BaseMapper<Log> {
}
