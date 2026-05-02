package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Discussion;
import cn.org.alan.exam.model.form.discussion.DiscussionForm;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * 讨论帖表单与 {@link Discussion} 实体映射。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel="spring")
public interface DiscussionConverter {

    /** 发帖/编辑讨论：表单转实体。 */
    Discussion formToEntity(DiscussionForm discussion);
}
