package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.UserExerciseRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户在题库下的刷题汇总记录（与单次答题明细 {@link IExerciseRecordService} 区分）；当前无自定义方法。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IUserExerciseRecordService extends IService<UserExerciseRecord> {

}
