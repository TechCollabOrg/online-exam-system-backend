package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.NoticeGradeMapper;
import cn.org.alan.exam.model.entity.NoticeGrade;
import cn.org.alan.exam.service.INoticeGradeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 公告与可见班级关联 {@link cn.org.alan.exam.model.entity.NoticeGrade} 的中间表服务。
 *
 * @author WeiJin
 */
@Service
public class NoticeGradeServiceImpl extends ServiceImpl<NoticeGradeMapper, NoticeGrade> implements INoticeGradeService {

}
