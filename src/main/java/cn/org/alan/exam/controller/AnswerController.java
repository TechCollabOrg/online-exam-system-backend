package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.group.AnswerGroup;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.answer.CorrectAnswerFrom;
import cn.org.alan.exam.model.vo.answer.AnswerExamVO;
import cn.org.alan.exam.model.vo.answer.UncorrectedUserVO;
import cn.org.alan.exam.model.vo.answer.UserAnswerDetailVO;
import cn.org.alan.exam.service.IAutoScoringService;
import cn.org.alan.exam.service.IManualScoreService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 主观题阅卷：查看考生作答、批量打分、待阅试卷与待阅考生分页。
 *
 * @author WeiJin
 */
@Api(tags = "答卷管理接口")
@RestController
@RequestMapping("/api/answers")
public class AnswerController {

    @Resource
    private IManualScoreService manualScoreService;

    @Resource
    private IAutoScoringService autoScoringService;

    /** GET 指定用户在某场考试的主观题作答明细。 */
    @ApiOperation("试卷查询信息")
    @GetMapping("/detail")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<UserAnswerDetailVO>> getDetail(@RequestParam Integer userId,
                                                      @RequestParam Integer examId) {
        return manualScoreService.getDetail(userId, examId);
    }

    /** POST 触发指定考生在某场考试的主观题 AI 阅卷（异步，结果写入 ai_score / ai_reason）。 */
    @ApiOperation("触发AI阅卷")
    @PostMapping("/ai-score")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> triggerAiScore(@RequestParam Integer examId,
                                         @RequestParam Integer userId) {
        autoScoringService.autoScoringExam(examId, userId);
        return Result.success("AI 阅卷任务已提交，请稍后刷新页面查看建议分数");
    }

    /** PUT 批量提交简答题分数；Body 校验分组 {@link AnswerGroup.CorrectGroup}。 */
    @ApiOperation("批改试卷")
    @PutMapping("/correct")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> Correct(@RequestBody @Validated(AnswerGroup.CorrectGroup.class) List<CorrectAnswerFrom> correctAnswerFroms) {
        return manualScoreService.correct(correctAnswerFroms);
    }

    /** GET 教师待阅卷的考试分页。 */
    @ApiOperation("分页查找待阅卷考试")
    @GetMapping("/exam/page")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<AnswerExamVO>> examPage(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                @RequestParam(value = "examName", required = false) String examName) {
        return manualScoreService.examPage(pageNum, pageSize, examName);
    }

    /** GET 某场考试下仍未完成阅卷的考生分页。 */
    @ApiOperation("查询待批阅的用户")
    @GetMapping("/exam/stu")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<UncorrectedUserVO>> stuExamPage(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                        @RequestParam(value = "examId") Integer examId,
                                                        @RequestParam(value = "realName", required = false) String realName) {
        return manualScoreService.stuExamPage(pageNum, pageSize, examId, realName);
    }
}
