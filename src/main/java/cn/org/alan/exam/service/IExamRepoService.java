package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.ExamRepo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 试卷与抽题题库关联；继承通用 CRUD，标识考试从哪些题库抽题。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IExamRepoService extends IService<ExamRepo> {

}
