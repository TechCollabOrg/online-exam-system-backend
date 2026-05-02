package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.OptionMapper;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.service.IOptionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 试题选项实体 {@link cn.org.alan.exam.model.entity.Option} 的基础数据访问层封装。
 *
 * @author WeiJin
 */
@Service
public class OptionServiceImpl extends ServiceImpl<OptionMapper, Option> implements IOptionService {

}
