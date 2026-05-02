package cn.org.alan.exam.service;

import cn.org.alan.exam.model.entity.NoticeGrade;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 公告与班级的定向关联；继承通用 CRUD，维护可见班级范围。
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface INoticeGradeService extends IService<NoticeGrade> {

}
