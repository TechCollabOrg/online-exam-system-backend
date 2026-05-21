package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.InviteCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 邀请码表 Mapper。
 */
public interface InviteCodeMapper extends BaseMapper<InviteCode> {

    /**
     * 原子扣减一次可用次数（仅在未用尽、启用、未过期时成功）。
     *
     * @return 影响行数，1 表示成功
     */
    @Update("UPDATE t_invite_code SET used_count = used_count + 1 " +
            "WHERE id = #{id} AND is_deleted = 0 AND status = 1 " +
            "AND used_count < max_uses " +
            "AND (expire_time IS NULL OR expire_time > NOW())")
    int consumeOnce(@Param("id") Integer id);
}
