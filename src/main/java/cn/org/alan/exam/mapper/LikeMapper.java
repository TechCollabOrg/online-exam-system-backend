package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.Like;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * 讨论点赞关联表 Mapper（{@link cn.org.alan.exam.model.entity.Like}），使用 BaseMapper 通用 CRUD。
 *
 * @author WeiJin
 * @version 1.0
 * @since 2025/4/16 22:25
 */
@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
