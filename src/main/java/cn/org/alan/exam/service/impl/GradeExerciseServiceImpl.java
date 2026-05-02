package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.GradeExerciseMapper;
import cn.org.alan.exam.model.entity.GradeExercise;
import cn.org.alan.exam.service.IGradeExerciseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 班级与练习题关联 {@link cn.org.alan.exam.model.entity.GradeExercise} 的基础服务。
 *
 * @author WeiJin
 */
@Service
public class GradeExerciseServiceImpl extends ServiceImpl<GradeExerciseMapper, GradeExercise> implements IGradeExerciseService {

}
