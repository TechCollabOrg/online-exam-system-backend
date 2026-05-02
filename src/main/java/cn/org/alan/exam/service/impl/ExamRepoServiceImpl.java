package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.ExamRepoMapper;
import cn.org.alan.exam.model.entity.ExamRepo;
import cn.org.alan.exam.service.IExamRepoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 考试与题库引用关系 {@link cn.org.alan.exam.model.entity.ExamRepo} 的基础服务。
 *
 * @author WeiJin
 */
@Service
public class ExamRepoServiceImpl extends ServiceImpl<ExamRepoMapper, ExamRepo> implements IExamRepoService {

}
