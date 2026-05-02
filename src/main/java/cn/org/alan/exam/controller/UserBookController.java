package cn.org.alan.exam.controller;


import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.userbook.ReUserBookForm;
import cn.org.alan.exam.model.vo.userbook.*;
import cn.org.alan.exam.service.IUserBookService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 错题本：分页、按考试列错题、单题巩固作答。
 *
 * @author Alan
 */
@Api(tags = "错题本相关接口")
@RestController
@RequestMapping("/api/userbooks")
public class UserBookController {

    @Resource
    private IUserBookService userBookService;

    /** GET 学生错题本分页；可按考试名称筛选。 */
    @ApiOperation("学生错题本分页查询")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<IPage<UserPageBookVO>> getPage(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                 @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                 @RequestParam(value = "examName", required = false) String examName) {

        return userBookService.getPage(pageNum, pageSize, examName);
    }

    /** GET 某场考试下的错题 ID 列表摘要。 */
    @ApiOperation("查询错题本错题id列表")
    @GetMapping("/question/list/{examId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<List<ReUserExamBookVO>> getReUserExamBook(@PathVariable("examId") Integer examId) {
        return userBookService.getReUserExamBook(examId);
    }

    /** GET 错题单题详情（题干选项等）。 */
    @ApiOperation("查询单题")
    @GetMapping("/question/single/{quId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<BookOneQuVO> getBookOne(@PathVariable("quId") Integer quId) {
        return userBookService.getBookOne(quId);
    }

    /** POST 错题复习作答提交。 */
    @ApiOperation("填充答案")
    @PostMapping("/full-book")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin','role_student')")
    public Result<AddBookAnswerVO> addBookAnswer(@RequestBody ReUserBookForm reUserBookForm) {
        return userBookService.addBookAnswer(reUserBookForm);
    }
}
