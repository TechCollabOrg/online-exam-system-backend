package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.service.IExamQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


/**
 * 试卷与题目关联 {@link cn.org.alan.exam.model.entity.ExamQuestion}（组卷、排序、分值类型）的基础 CRUD。
 *
 * @author Alan
 */
@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements IExamQuestionService {


}
