package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.stat.AllStatsVO;
import cn.org.alan.exam.model.vo.stat.DailyVO;
import cn.org.alan.exam.model.vo.stat.GradeExamVO;
import cn.org.alan.exam.model.vo.stat.GradeStudentVO;
import cn.org.alan.exam.service.IStatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 看板统计：班级人数、考试场次、总览计数与个人日活曲线。
 *
 * @author Alan
 */
@Api(tags = "统计数据相关接口")
@RestController
@RequestMapping("/api/stat")
public class StatController {

    @Resource
    private IStatService statService;

    /** GET 各班学生人数分布。 */
    @ApiOperation("各班级人数统计")
    @GetMapping("/student")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<GradeStudentVO>> getStudentGradeCount() {
        return statService.getStudentGradeCount();
    }

    /** GET 各班关联考试场次统计。 */
    @ApiOperation("各班试卷统计")
    @GetMapping("/exam")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<GradeExamVO>> getExamGradeCount() {
        return statService.getExamGradeCount();
    }

    /** GET 班级/试卷/试题总数总览（角色分支见 Service）。 */
    @ApiOperation("统计所有班级、试卷、试题数量")
    @GetMapping("/allCounts")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<AllStatsVO> getAllCount() {
        return statService.getAllCount();
    }

    /** GET 当前用户日在线/登录序列（教师管理员与学生均可调，数据口径按用户区分）。 */
    @ApiOperation("获取用户登录时间统计")
    @GetMapping("/daily")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<List<DailyVO>> getDaily() {
        return statService.getDaily();
    }

}
