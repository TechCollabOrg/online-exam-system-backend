package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.score.GradeScoreVO;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;
import cn.org.alan.exam.model.vo.score.UserScoreVO;
import cn.org.alan.exam.service.IExamQuAnswerService;
import cn.org.alan.exam.service.IUserExamsScoreService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 成绩查询、单题分析与 Excel 导出。
 *
 * @author WeiJin
 */
@Api(tags = "成绩相关接口")
@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Resource
    private IUserExamsScoreService iUserExamsScoreService;
    @Resource
    private IExamQuAnswerService iExamQuAnswerService;

    /** GET 某班某场考试的成绩分页。 */
    @ApiOperation("分页获取成绩信息")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<UserScoreVO>> pagingScore(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                  @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                  @RequestParam(value = "gradeId") Integer gradeId,
                                                  @RequestParam(value = "examId") Integer examId,
                                                  @RequestParam(value = "realName", required = false) String realName) {
        return iUserExamsScoreService.pagingScore(pageNum, pageSize, gradeId, examId, realName);
    }

    /** GET 单场考试单题正确率等统计。 */
    @ApiOperation("获取某场考试某题作答情况")
    @GetMapping("/question/{examId}/{questionId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<QuestionAnalyseVO> questionAnalyse(@PathVariable("examId") Integer examId,
                                                     @PathVariable("questionId") Integer questionId) {
        return iExamQuAnswerService.questionAnalyse(examId, questionId);
    }

    /** GET 教师侧成绩统计分页（按考试标题、班级过滤）。 */
    @ApiOperation("根据班级分析考试情况")
    @GetMapping("/getExamScore")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<GradeScoreVO>> getExamScoreInfo(
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "examTitle", required = false) String examTitle,
            @RequestParam(value = "gradeId", required = false) Integer gradeId) {
        return iUserExamsScoreService.getExamScoreInfo(pageNum, pageSize, examTitle, gradeId);
    }

    /** GET 导出某班某场考试成绩 Excel（直接写 response 流）。 */
    @ApiOperation("成绩导出")
    @GetMapping("/export/{examId}/{gradeId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public void scoreExport(HttpServletResponse response, @PathVariable("examId") Integer examId, @PathVariable("gradeId") Integer gradeId) {
        iUserExamsScoreService.exportScores(response, examId, gradeId);
    }

}
