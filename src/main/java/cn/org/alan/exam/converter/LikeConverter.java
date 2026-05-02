package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Like;
import cn.org.alan.exam.model.form.like.LikeForm;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * 点赞操作表单与 {@link Like} 实体映射。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel = "spring")
public interface LikeConverter {
    /** 用户点赞/取消场景下的表单转实体。 */
    Like formToEntity(LikeForm likeForm);
}
