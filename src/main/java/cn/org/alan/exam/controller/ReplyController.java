package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.Reply;
import cn.org.alan.exam.model.form.reply.ReplyForm;
import cn.org.alan.exam.model.vo.reply.ReplyVo;
import cn.org.alan.exam.service.IReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 讨论帖回复：新增、删除、按讨论 ID 分页/排序查询。
 *
 * @author WeiJin
 */
@RestController
@Api(tags = "回复相关接口")
@RequestMapping("/api/reply")
public class ReplyController {

    @Resource
    private IReplyService replyService;

    /** POST 发表评论/回复。 */
    @PostMapping("add")
    @ApiOperation("添加回复")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher')")
    public Result<Reply> addReply(@RequestBody ReplyForm replyForm) {
        Reply reply = replyService.addReply(replyForm);
        return Result.success("回复成功", reply);
    }

    /** DELETE 删除回复（楼主或本人，逻辑在 Service）。 */
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher')")
    @ApiOperation("删除回复")
    @DeleteMapping("/delete/{id}")
    public Result<Integer> deleteReply(@PathVariable("id") Integer id) {
        Integer delId = replyService.deleteReply(id);
        return Result.success("删除成功", delId);
    }

    /**
     * GET 某讨论下回复列表；{@code orderBy}：1 时间升序、2 时间降序、3 点赞升序、4 点赞降序（与 Mapper 约定一致）。
     */
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher')")
    @ApiOperation("根据讨论id获取评论")
    @GetMapping("/query/{orderBy}/{id}")
    public Result<List<ReplyVo>> queryReplyByDiscussionId(@PathVariable("orderBy") Integer orderBy, @PathVariable("id") Integer id) {
        List<ReplyVo> replyVos = replyService.queryReplyByDiscussionId(orderBy, id);
        return Result.success("查询成功", replyVos);
    }

}
