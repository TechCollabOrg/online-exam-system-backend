package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Repo;
import cn.org.alan.exam.model.vo.record.ExerciseRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

/**
 * 练习/刷题记录分页：将 {@link Repo} 分页数据映射为 {@link ExerciseRecordVO} 分页（方法名沿用以兼容调用方）。
 *
 * @author Alan
 */
@Component
@Mapper(componentModel = "spring")
public interface RecordConverter {

    /** 分页映射为练习记录 VO（字段按 MapStruct 同名/自定义策略对齐）。 */
    Page<ExerciseRecordVO> pageRepoEntityToVo(Page<Repo> page);

}
