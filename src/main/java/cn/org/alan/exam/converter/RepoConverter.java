package cn.org.alan.exam.converter;

import cn.org.alan.exam.model.entity.Repo;
import cn.org.alan.exam.model.vo.repo.RepoVO;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 题库 {@link Repo} 与列表展示 VO 的映射。
 *
 * @author WeiJin
 */
@Component
@Mapper(componentModel = "spring")
public interface RepoConverter {

    /** 题库实体列表转前端表格/下拉所需 VO 列表。 */
    List<RepoVO> listEntityToVo(List<Repo> list);

}
