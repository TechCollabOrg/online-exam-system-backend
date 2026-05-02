package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.ExamQuestion;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 试卷与试题关联；继承通用 CRUD，组卷写入由实现类封装。
 *
 * @author Alan
 * @since 2024/4/7
 */
public interface IExamQuestionService extends IService<ExamQuestion> {
}
