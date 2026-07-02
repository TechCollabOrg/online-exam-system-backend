package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.ExamUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试与指定学生关联 Mapper。
 */
public interface ExamUserMapper extends BaseMapper<ExamUser> {

    /**
     * 批量绑定考试与指定学生。
     */
    Integer addExamUsers(@Param("examId") Integer examId, @Param("userIds") List<Integer> userIds);
}
