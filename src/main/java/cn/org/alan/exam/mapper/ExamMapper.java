package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.vo.answer.AnswerExamVO;
import cn.org.alan.exam.model.vo.record.ExamRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 考试（试卷）主表 Mapper：阅卷侧考试分页、学生/教师/管理员考试记录分页。
 *
 * @author Alan
 * @since 2024-03-21
 */
public interface ExamMapper extends BaseMapper<Exam> {

    /**
     * 分页查询当前用户创建的、且包含待阅主观题的考试列表（考试 ID、标题、是否需阅卷等）。
     *
     * @param page    分页参数
     * @param userId  用户 ID
     * @param role    角色编码（SQL 中用于过滤）
     * @param examName 考试名称模糊条件，可空
     * @return 分页结果
     */
    IPage<AnswerExamVO> selectMarkedList(@Param("page") IPage<AnswerExamVO> page, @Param("userId") Integer userId, String role, String examName);

    /**
     * 查询学生的考试记录
     *
     * @param page     分页对象
     * @param userId   用户ID
     * @param examName 考试名称
     * @param isASC    是否升序
     * @return 分页结果
     */
    Page<ExamRecordVO> getExamRecordPage(Page<ExamRecordVO> page, Integer userId, String examName, Boolean isASC);

    /**
     * 查询教师创建的考试记录
     *
     * @param page     分页对象
     * @param userId   用户ID
     * @param examName 考试名称
     * @param isASC    是否升序
     * @return 分页结果
     */
    Page<ExamRecordVO> getTeacherExamRecordPage(Page<ExamRecordVO> page, Integer userId, String examName, Boolean isASC);

    /**
     * 查询所有考试记录（管理员）
     *
     * @param page     分页对象
     * @param examName 考试名称
     * @param isASC    是否升序
     * @return 分页结果
     */
    Page<ExamRecordVO> getAllExamRecordPage(Page<ExamRecordVO> page, String examName, Boolean isASC);

}
