package cn.org.alan.exam.controller;


import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.exam.ExamAddForm;
import cn.org.alan.exam.model.form.exam.ExamUpdateForm;
import cn.org.alan.exam.model.form.exam_qu_answer.ExamQuAnswerAddForm;
import cn.org.alan.exam.model.vo.exam.*;
import cn.org.alan.exam.model.vo.record.ExamRecordDetailVO;
import cn.org.alan.exam.service.IExamService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 试卷生命周期与学生作答 HTTP 入口：创建维护需教师或管理员；开考、答题、交卷等支持学生角色。
 * <p>路径前缀 {@code /api/exams}，具体权限见各方法 {@link PreAuthorize}。</p>
 *
 * @author Alan
 */
@Api(tags = "考试管理相关接口")
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Resource
    private IExamService examService;

    /** POST 创建试卷（组卷、班级、题库关联等在 Service 内完成）。 */
    @ApiOperation("创建考试")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> createExam(@Validated @RequestBody ExamAddForm examAddForm) {
        return examService.createExam(examAddForm);
    }

    /** GET 开课：写入进行中成绩记录，防重复开考。 */
    @ApiOperation("开始考试")
    @GetMapping("/start")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<String> startExam(@RequestParam("examId") @NotNull Integer examId) {
        return examService.startExam(examId);
    }

    /** PUT 更新试卷基础信息（路径 {@code id}）。 */
    @ApiOperation("修改考试")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> updateExam(@Validated @RequestBody ExamUpdateForm examUpdateForm, @PathVariable("id") @NotNull Integer id) {
        return examService.updateExam(examUpdateForm, id);
    }

    /** DELETE 批量删除试卷；{@code ids} 支持逗号分隔，须匹配正则校验。 */
    @ApiOperation("删除考试")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> deleteExam(@PathVariable("ids") @Pattern(regexp = "^\\d+(,\\d+)*$|^\\d+$") String ids) {
        return examService.deleteExam(ids);
    }

    /** GET 试卷管理分页（教师只看本人创建，管理员看全部）。 */
    @ApiOperation("教师分页查找考试列表")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<ExamVO>> getPagingExam(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                               @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                               @RequestParam(value = "title", required = false) String title) {
        return examService.getPagingExam(pageNum, pageSize, title);
    }

    /** GET 考试中题型分组题目列表与倒计时（须处于进行中状态）。 */
    @ApiOperation("获取考试题目id列表")
    @GetMapping("/question/list/{examId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<ExamQuestionListVO> getQuestionList(@PathVariable("examId") @NotBlank Integer examId) {
        return examService.getQuestionList(examId);
    }

    /** GET 考试中单题题干、选项及作答勾选状态。 */
    @ApiOperation("获取单题信息")
    @GetMapping("/question/single")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<ExamQuDetailVO> getQuestionSingle(@RequestParam("examId") Integer examId,
                                                    @RequestParam("questionId") Integer questionId) {
        return examService.getQuestionSingle(examId, questionId);
    }

    /** GET 考试中题目导航汇总（含「我的答案」展示）。 */
    @ApiOperation("题目汇总")
    @GetMapping("/collect/{id}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<List<ExamQuCollectVO>> getCollect(@PathVariable("id") @NotNull Integer examId) {
        return examService.getCollect(examId);
    }


    /** GET 试卷静态详情（非作答中界面）。 */
    @ApiOperation("获取考试详情信息")
    @GetMapping("/detail")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<ExamDetailVO> getDetail(@RequestParam("examId") @NotBlank Integer examId) {
        return examService.getDetail(examId);
    }

    /** GET 按班级/角色可见的考试列表分页（学生、教师、管理员 SQL 分支不同）。 */
    @ApiOperation("根据班级获得考试")
    @GetMapping("/grade")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<IPage<ExamGradeListVO>> getGradeExamList(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                           @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                           @RequestParam(value = "title", required = false) String title,
                                                           @RequestParam(value = "isASC", required = false, defaultValue = "false") Boolean isASC) {
        return examService.getGradeExamList(pageNum, pageSize, title, isASC);
    }

    /** PUT 上报切屏次数；超限可由服务端触发自动交卷。 */
    @ApiOperation("考试作弊次数添加")
    @PutMapping("/cheat/{examId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<Integer> addCheat(@PathVariable("examId") @NotNull Integer examId) {
        return examService.addCheat(examId);
    }

    /** POST 保存或更新本场考试某一题作答。 */
    @ApiOperation("填充答案")
    @PostMapping("/full-answer")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<String> addAnswer(@Validated @RequestBody ExamQuAnswerAddForm examQuAnswerForm) {
        return examService.addAnswer(examQuAnswerForm);
    }

    /** GET 手动交卷（路径参数 examId）。 */
    @ApiOperation("交卷操作")
    @GetMapping(value = "/hand-exam/{examId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<ExamQuDetailVO> handleExam(@PathVariable("examId") @NotNull Integer examId) {
        return examService.handExam(examId);
    }

    /** GET 考后试题解析列表（题干、选项、正确答案等）。 */
    @ApiOperation("查看考试详情")
    @GetMapping(value = "/details/{examId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<List<ExamRecordDetailVO>> details(@PathVariable("examId") @NotNull Integer examId) {
        return examService.details(examId);
    }
}