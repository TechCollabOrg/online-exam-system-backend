package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.UserExerciseRecordMapper;
import cn.org.alan.exam.model.entity.UserExerciseRecord;
import cn.org.alan.exam.service.IUserExerciseRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户练习题作答流水：无自定义方法，继承 MyBatis-Plus {@link ServiceImpl} 默认 CRUD。
 *
 * @author WeiJin
 */
@Service
public class UserExerciseRecordServiceImpl extends ServiceImpl<UserExerciseRecordMapper, UserExerciseRecord> implements IUserExerciseRecordService {

}
