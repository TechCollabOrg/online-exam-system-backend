package cn.org.alan.exam.controller;


import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.grade.GradeForm;
import cn.org.alan.exam.model.vo.grade.GradeVO;
import cn.org.alan.exam.service.IGradeService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级与师生归属：管理员维护班级实体，教师加入/退出任教，学生退出本班。
 *
 * @author Alan
 */
@Api(tags = "班级管理相关接口")
@RestController
@RequestMapping("/api/grades")
public class GradeController {

    @Resource
    private IGradeService gradeService;

    /** GET 班级分页（教师限所加入班级）。 */
    @ApiOperation("分页查询班级")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<GradeVO>> getGrade(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                           @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                           @RequestParam(value = "gradeName", required = false) String gradeName) {
        return gradeService.getPaging(pageNum, pageSize, gradeName);
    }

    /** POST 新建班级（仅管理员）。 */
    @ApiOperation("新增班级")
    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> addGrade(@Validated @RequestBody GradeForm gradeForm) {
        return gradeService.addGrade(gradeForm);
    }

    /** PUT 修改班级名称等（仅管理员）。 */
    @ApiOperation("修改班级")
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> updateGrade(@PathVariable("id") @NotNull Integer id, @Validated @RequestBody GradeForm gradeForm) {
        return gradeService.updateGrade(id, gradeForm);
    }

    /** DELETE 删除班级（仅管理员）。 */
    @ApiOperation("删除班级")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> deleteGrade(@PathVariable("id") @NotNull Integer id) {
        return gradeService.deleteGrade(id);
    }

    /** PATCH 批量将用户移出班级（路径 ids 多为逗号分隔用户 ID）。 */
    @ApiOperation("退出班级")
    @PatchMapping("/remove/{ids}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<String> removeUserGrade(@PathVariable("ids") @NotBlank String ids) {
        return gradeService.removeUserGrade(ids);
    }

    /** GET 班级全量列表（下拉/筛选；教师限范围）。 */
    @ApiOperation("获取所有班级列表")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<GradeVO>> getAllGrade() {
        return gradeService.getAllGrade();
    }

    /** GET 教师凭口令加入班级任教。 */
    @ApiOperation("老师加入班级")
    @GetMapping("/teacher/join")
    @PreAuthorize("hasAnyAuthority('role_teacher')")
    public Result teacherJoinClass(@RequestParam("code") String code) {
        return gradeService.teacherJoinClass(code);
    }

    /** DELETE 教师退出指定班级任教关系。 */
    @ApiOperation("老师退出班级")
    @DeleteMapping("/teacher/exit/{gradeId}")
    @PreAuthorize("hasAnyAuthority('role_teacher')")
    public Result teacherExitClass(@PathVariable("gradeId") String gradeId) {
        return gradeService.teacherExitClass(gradeId);
    }

    /** PUT 学生退出当前班级。 */
    @ApiOperation("学生退出班级")
    @PutMapping("/user/exit")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result userExitGrade() {
        return gradeService.userExitGrade();
    }
}
