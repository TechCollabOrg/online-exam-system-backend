package cn.org.alan.exam.model.form.notice;

import cn.org.alan.exam.common.result.Result;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 发布公告或更新公告标题、正文、可见范围等。
 *
 * @author Alan
 * @since 2024/3/28
 */
@Data
public class NoticeForm {

    /**
     * ID
     */
    private Integer id;

    /**
     * 公告标题
     */
    @NotBlank
    private String title;

    /**
     * 图片地址
     */
    private String image;

    /**
     * 公告内容
     */
    @NotBlank
    private String content;

    /**
     * 创建用户ID
     */
    private Integer userId;

    /**
     * 公告班级
     */
    private String gradeIds;

    /**
     * 是否公开 教师：所有班级用户 管理员：所有用户
     */
    private Integer isPublic;


}
