package cn.org.alan.exam.model.vo.answer;

import lombok.Data;

/**
 * 某场考试指定班级（或全部关联班级）中未交卷的学生。
 */
@Data
public class AbsentUserVO {

    private Integer userId;

    private String userName;

    private Integer gradeId;

    private String gradeName;
}
