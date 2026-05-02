package cn.org.alan.exam.common.group;

/**
 * 试题表单校验分组。
 *
 * @author WeiJin
 * @since 2024/4/1
 */
public interface QuestionGroup {

    /** 新增试题时的字段校验分组。 */
    interface QuestionAddGroup extends QuestionGroup {
    }
}
