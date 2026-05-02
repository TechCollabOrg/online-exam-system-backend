package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库多级分类表 Mapper，继承 BaseMapper 完成分类 CRUD。
 *
 * @author Moxuec
 * @since 2025-04-09
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}