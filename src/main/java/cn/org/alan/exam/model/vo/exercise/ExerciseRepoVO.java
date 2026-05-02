package cn.org.alan.exam.model.vo.exercise;

import lombok.Data;

/**
 * 学生端可刷题库分页行：标题、分类、题目数量等。
 *
 * @author WeiJin
 * @since 2024/5/6
 */
@Data
public class ExerciseRepoVO {
    private Integer id;
    // 题库标题
    private String repoTitle;
    // 总题数
    private Integer totalCount;
    private Integer exerciseCount;
    // 分类相关字段
    private Integer categoryId;
    private String categoryName;
    private Integer parentCategoryId;
    private String parentCategoryName;
}
