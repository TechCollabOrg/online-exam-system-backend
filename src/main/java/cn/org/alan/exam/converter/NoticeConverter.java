package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Notice;
import cn.org.alan.exam.model.form.notice.NoticeForm;
import cn.org.alan.exam.model.vo.notice.NoticeVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * 公告表单、分页与详情 VO 映射。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel="spring")
public interface NoticeConverter {

    /** 发布公告：表单转实体。 */
    Notice formToEntity(NoticeForm noticeForm);

    /** 公告分页列表实体转 VO。 */
    Page<NoticeVO> pageEntityToVo(Page<Notice> noticePage);

    /** 单条公告实体转展示 VO。 */
    NoticeVO NoticeToNoticeVO(Notice notice);
}
