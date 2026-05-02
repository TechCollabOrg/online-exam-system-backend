package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.converter.DiscussionConverter;
import cn.org.alan.exam.mapper.DiscussionMapper;
import cn.org.alan.exam.mapper.ReplyMapper;
import cn.org.alan.exam.model.entity.Discussion;
import cn.org.alan.exam.model.form.discussion.DiscussionForm;
import cn.org.alan.exam.model.vo.discussion.DiscussionDetailVo;
import cn.org.alan.exam.model.vo.discussion.PageDiscussionVo;
import cn.org.alan.exam.service.IDiscussionService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 讨论帖：创建、删除、楼主视角分页、详情及按班级分页列表。
 *
 * @author WeiJin
 */
@Service
public class DiscussionServiceImpl extends ServiceImpl<DiscussionMapper, Discussion> implements IDiscussionService {

    @Resource
    private DiscussionConverter discussionConverter;
    @Resource
    private ReplyMapper replyMapper;

    /** 发帖：表单转实体并写入当前用户为发布人。 */
    @Override
    public Discussion createDiscussion(DiscussionForm discussionForm) {
        // 入参转为实体
        Discussion discussion = discussionConverter.formToEntity(discussionForm);
        // 获取当前用户id
        Integer userId = SecurityUtil.getUserId();
        // 填充发布人
        discussion.setUserId(userId);
        int inserted = baseMapper.insert(discussion);
        if (inserted > 0) {
            return discussion;
        }
        throw new RuntimeException("创建讨论失败");
    }


    /**
     * 删除讨论帖（存在性校验）；事务内执行，调用方需另行清理回复与点赞（若业务未在此处级联）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDiscussion(Integer id) {
        // 先做一个非空判断
        Discussion discussion = baseMapper.selectById(id);
        if (discussion == null) {
            throw new RuntimeException("该id无对应的讨论");
        }
        // 删除讨论
        int deleted = baseMapper.deleteById(id);
        // 删除该讨论的所有回复

        if (deleted > 0) {
            // 删除成功返回id
            return id;
        }
        throw new RuntimeException("删除讨论失败");
    }

    /** 当前用户发布的讨论分页，可按标题、班级筛选。 */
    @Override
    public Page<PageDiscussionVo> getOwnerDiscussions(String title, Integer gradeId, Integer currentPage, Integer size) {
        Page<PageDiscussionVo> page = new Page<>(currentPage, size);
        return baseMapper.selectOwnerPage(page, SecurityUtil.getUserId(), title, gradeId);
    }


    /** 讨论详情（含回复统计等由 Mapper 组装）。 */
    @Override
    public DiscussionDetailVo getDiscussionDetail(Integer id) {
        return baseMapper.selectDetail(id);
    }

    /** 当前用户所在班级的讨论列表分页。 */
    @Override
    public Page<PageDiscussionVo> pageDiscussionByGrade(String title, Integer currentPage, Integer size) {
        Page<PageDiscussionVo> page = new Page<>(currentPage, size);
        return baseMapper.selectDiscussionByGradePage(page, title, SecurityUtil.getGradeId());
    }


}
