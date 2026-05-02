package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.Category;
import cn.org.alan.exam.model.vo.category.CategoryVO;
import cn.org.alan.exam.service.ICategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 题库多级分类的增删改查与树形读取。
 *
 * @author Moxuec
 */
@Api(tags = "题库分类管理相关接口")
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private ICategoryService categoryService;

    /** POST 新增分类。 */
    @PostMapping
    @ApiOperation("添加分类")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> addCategory(@Validated @RequestBody Category category) {
        return categoryService.addCategory(category);
    }

    /** PUT 更新分类。 */
    @PutMapping("/{id}")
    @ApiOperation("修改分类")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> updateCategory(@Validated @RequestBody Category category, @PathVariable("id") Integer id) {
        return categoryService.updateCategory(category, id);
    }

    /** DELETE 删除分类（有子节点时 Service 侧拒绝）。 */
    @DeleteMapping("/{id}")
    @ApiOperation("删除分类")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<String> deleteCategory(@PathVariable("id") Integer id) {
        return categoryService.deleteCategory(id);
    }

    /** GET 完整分类树。 */
    @GetMapping("/tree")
    @ApiOperation("获取分类树")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<List<CategoryVO>> getCategoryTree() {
        return categoryService.getCategoryTree();
    }

    /** GET 一级分类列表。 */
    @GetMapping("/first-level")
    @ApiOperation("获取一级分类")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<List<CategoryVO>> getFirstLevelCategories() {
        return categoryService.getFirstLevelCategories();
    }

    /** GET 指定父节点下的子分类。 */
    @GetMapping("/children/{parentId}")
    @ApiOperation("获取子分类")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<List<CategoryVO>> getChildCategories(@PathVariable("parentId") Integer parentId) {
        return categoryService.getChildCategories(parentId);
    }
}
