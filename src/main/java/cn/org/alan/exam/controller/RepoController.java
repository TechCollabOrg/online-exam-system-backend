package cn.org.alan.exam.controller;


import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.Repo;
import cn.org.alan.exam.model.vo.repo.RepoListVO;
import cn.org.alan.exam.model.vo.repo.RepoVO;
import cn.org.alan.exam.service.IRepoService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库维护与后台分页列表；创建人等信息由 Service 结合登录态处理。
 *
 * @author WeiJin
 */
@Api(tags = "题库管理相关接口")
@RestController
@RequestMapping("/api/repo")
public class RepoController {

    @Resource
    private IRepoService iRepoService;

    /** POST 新建题库 Body 为 {@link Repo}。 */
    @PostMapping
    @ApiOperation("添加题库")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> addRepo(@Validated @RequestBody Repo repo) {
        return iRepoService.addRepo(repo);
    }

    /** PUT 更新题库。 */
    @ApiOperation("修改题库")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> updateRepo(@Validated @RequestBody Repo repo, @PathVariable("id") Integer id) {
        return iRepoService.updateRepo(repo, id);
    }

    /** DELETE 删除题库并解除下属题目的题库关联。 */
    @ApiOperation("根据题库id删除题库")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> deleteRepoById(@PathVariable("id") Integer id) {
        return iRepoService.deleteRepoById(id);
    }

    /** GET 题库下拉简要列表（教师本人 / 管理员全部）。 */
    @ApiOperation("获取所有题库")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<RepoListVO>> getRepoList(@RequestParam(value = "repoTitle", required = false) String repoTitle) {
        return iRepoService.getRepoList(repoTitle);
    }

    /** GET 题库分页，可选分类筛选。 */
    @ApiOperation("分页查询题库")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<RepoVO>> pagingRepo(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                            @RequestParam(value = "title", required = false) String title,
                                            @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        return iRepoService.pagingRepo(pageNum, pageSize, title, categoryId);
    }

    /** GET 按分类（含子分类）筛选题库分页。 */
    @ApiOperation("根据分类ID查询题库")
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<IPage<RepoVO>> getReposByCategory(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        return iRepoService.getReposByCategory(categoryId, pageNum, pageSize);
    }

}
