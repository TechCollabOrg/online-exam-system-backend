package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.GradeExercise;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 班级与刷题题库分配；继承通用 CRUD，维护班级可练习的题库关系。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IGradeExerciseService extends IService<GradeExercise> {

}
