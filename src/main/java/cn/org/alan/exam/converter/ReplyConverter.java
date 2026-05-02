package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Reply;
import cn.org.alan.exam.model.form.reply.ReplyForm;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * 讨论区回复表单与 {@link Reply} 实体映射。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel = "spring")
public interface ReplyConverter {
    /** 发表评论/回复时 DTO 转持久化对象。 */
    Reply formToEntity(ReplyForm replyForm);
}
