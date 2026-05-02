package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.Discussion;
import cn.org.alan.exam.model.form.discussion.DiscussionForm;
import cn.org.alan.exam.model.vo.discussion.DiscussionDetailVo;
import cn.org.alan.exam.model.vo.discussion.PageDiscussionVo;
import cn.org.alan.exam.service.IDiscussionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 教师发布讨论、学生按班级浏览与详情查询。
 *
 * @author Alan
 */
@RestController
@RequestMapping("/api/discussion")
@Api(tags = "讨论相关接口")
public class DiscussionController {

    @Resource
    private IDiscussionService discussionService;

    /** POST 教师创建讨论帖。 */
    @PostMapping("/add")
    @ApiOperation("创建讨论")
    @PreAuthorize("hasAnyAuthority('role_teacher')")
    public Result<Discussion> addDiscussion(@RequestBody @Validated DiscussionForm discussionForm) {
        Discussion discussion = discussionService.createDiscussion(discussionForm);
        return Result.success("创建成功", discussion);
    }

    /** DELETE 删除讨论（路径参数为主键）。 */
    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除讨论讨论")
    @PreAuthorize("hasAnyAuthority('role_teacher')")
    public Result<Integer> delDiscussion(@PathVariable("id") Integer id) {
        int delId = discussionService.deleteDiscussion(id);
        return Result.success("删除成功", delId);
    }

    /** GET 教师查看本人发布的讨论分页。 */
    @GetMapping("/query/page/owner")
    @ApiOperation("教师分页查询自己的发布的讨论")
    @PreAuthorize("hasAnyAuthority('role_teacher')")
    public Result<Page<PageDiscussionVo>> pageOwnerDiscussion(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "gradeId", required = false) Integer gradeId,
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
    ) {
        Page<PageDiscussionVo> ownerDiscussions = discussionService.getOwnerDiscussions(title, gradeId, currentPage, size);
        return Result.success("查询成功", ownerDiscussions);
    }

    /** GET 讨论详情（师生可视，权限见注解）。 */
    @GetMapping("/query/detail/{id}")
    @ApiOperation("获取讨论详情")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_student')")
    public Result<DiscussionDetailVo> pageOwnerDiscussion(@PathVariable("id") Integer id) {
        DiscussionDetailVo discussionDetail = discussionService.getDiscussionDetail(id);
        return Result.success("查询成功", discussionDetail);
    }

    /** GET 学生在本班范围内的讨论分页。 */
    @GetMapping("/query/page/student")
    @ApiOperation("学生根据班级id分页获取讨论")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<Page<PageDiscussionVo>> pageDiscussionByGrade(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
    ) {
        Page<PageDiscussionVo> pageDiscussionVoPage = discussionService.pageDiscussionByGrade(title, currentPage, size);
        return Result.success("查询成功", pageDiscussionVoPage);
    }
}
