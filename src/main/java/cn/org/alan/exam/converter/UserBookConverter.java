package cn.org.alan.exam.converter;


import cn.org.alan.exam.model.entity.UserBook;
import cn.org.alan.exam.model.vo.userbook.ReUserExamBookVO;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 错题本 {@link UserBook} 与「考试-错题」关联展示 VO 的映射。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel = "spring")
public interface UserBookConverter {

    /** 批量错题实体转前端列表 VO。 */
    List<ReUserExamBookVO> listEntityToVo(List<UserBook> list);

}
