package cn.org.alan.exam.common.group;

/**
 * 阅卷与作答提交相关的校验分组。
 *
 * @author WeiJin
 * @since 2024/4/29
 */
public interface AnswerGroup {

    /** 教师批量提交主观题得分时的列表元素校验分组。 */
    interface CorrectGroup extends AnswerGroup {
    }
}
