package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.record.ExamRecordDetailVO;
import cn.org.alan.exam.model.vo.record.ExamRecordVO;
import cn.org.alan.exam.model.vo.record.ExerciseRecordDetailVO;
import cn.org.alan.exam.model.vo.record.ExerciseRecordVO;
import cn.org.alan.exam.service.IExerciseRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 正式考试与刷题的历史记录查询（分页与详情）。
 *
 * @author Alan
 */
@Api(tags = "考试记录相关接口")
@RestController
@RequestMapping("/api/records")
public class RecordController {
    @Resource
    private IExerciseRecordService exerciseRecordService;

    /** GET 已参加考试分页（角色决定数据范围）。 */
    @ApiOperation("分页查询已考试试卷")
    @GetMapping("/exam/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<IPage<ExamRecordVO>> getExamRecordPage(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                         @RequestParam(value = "examName", required = false) String examName,
                                                         @RequestParam(value = "isASC", required = false, defaultValue = "false") Boolean isASC) {
        return exerciseRecordService.getExamRecordPage(pageNum, pageSize, examName, isASC);
    }

    /** GET 单场考试答题明细；{@code userId} 可选，默认当前用户。 */
    @ApiOperation("查询试卷详情")
    @GetMapping("/exam/detail")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<List<ExamRecordDetailVO>> getExamRecordDetail(@RequestParam(value = "examId",required = true) Integer examId,
                                                                @RequestParam(value = "userId",required = false)Integer userId) {
        return exerciseRecordService.getExamRecordDetail(examId,userId);
    }

    /** GET 已练习题库分页；{@code repoName} 模糊筛选。 */
    @ApiOperation("分页查询已考试刷题")
    @GetMapping("/exercise/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<IPage<ExerciseRecordVO>> getExerciseRecordPage(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                                 @RequestParam(value = "repoName", required = false) String repoName) {
        return exerciseRecordService.getExerciseRecordPage(pageNum, pageSize, repoName);
    }

    /**
     * GET 某题库刷题详情；请求参数名为 {@code repoId}，对应 Service 的题库 ID。
     */
    @ApiOperation("查询刷题详情")
    @GetMapping("/exercise/detail")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<List<ExerciseRecordDetailVO>> getExerciseRecordDetail(@RequestParam("repoId") Integer exerciseId) {
        return exerciseRecordService.getExerciseRecordDetail(exerciseId);
    }
}
