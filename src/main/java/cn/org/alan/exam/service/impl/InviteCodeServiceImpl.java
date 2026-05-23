package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.InviteCodeMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.InviteCode;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.form.invite.InviteCodeForm;
import cn.org.alan.exam.model.vo.invite.InviteCodeVO;
import cn.org.alan.exam.service.IInviteCodeService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 邀请码业务：仅管理员可维护；教师/管理员注册须凭码。
 */
@Service
public class InviteCodeServiceImpl extends ServiceImpl<InviteCodeMapper, InviteCode> implements IInviteCodeService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    @Resource
    private InviteCodeMapper inviteCodeMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public Result<String> create(InviteCodeForm form) {
        if (SecurityUtil.getRoleCode() != 3) {
            throw new ServiceRuntimeException("仅管理员可生成邀请码");
        }
        InviteCode row = new InviteCode();
        row.setCode(generateUniqueCode());
        row.setRoleId(form.getRoleId());
        row.setMaxUses(form.getMaxUses());
        row.setUsedCount(0);
        row.setStatus(STATUS_ENABLED);
        row.setRemark(form.getRemark());
        row.setExpireTime(form.getExpireTime());
        row.setCreatorId(SecurityUtil.getUserId());
        inviteCodeMapper.insert(row);
        return Result.success("邀请码已生成：" + row.getCode());
    }

    @Override
    public Result<IPage<InviteCodeVO>> paging(Integer pageNum, Integer pageSize, Integer roleId, Integer status) {
        if (SecurityUtil.getRoleCode() != 3) {
            throw new ServiceRuntimeException("仅管理员可查看邀请码");
        }
        Page<InviteCode> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InviteCode> w = new LambdaQueryWrapper<>();
        if (roleId != null) {
            w.eq(InviteCode::getRoleId, roleId);
        }
        if (status != null) {
            w.eq(InviteCode::getStatus, status);
        }
        w.orderByDesc(InviteCode::getCreateTime);
        IPage<InviteCode> raw = inviteCodeMapper.selectPage(page, w);
        Page<InviteCodeVO> voPage = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        voPage.setRecords(raw.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return Result.success("查询成功", voPage);
    }

    @Override
    public Result<String> disable(Integer id) {
        if (SecurityUtil.getRoleCode() != 3) {
            throw new ServiceRuntimeException("仅管理员可禁用邀请码");
        }
        InviteCode row = inviteCodeMapper.selectById(id);
        if (row == null) {
            throw new ServiceRuntimeException("邀请码不存在");
        }
        row.setStatus(STATUS_DISABLED);
        inviteCodeMapper.updateById(row);
        return Result.success("已禁用");
    }

    @Override
    @Transactional
    public Result<String> deleteBatch(String ids) {
        if (SecurityUtil.getRoleCode() != 3) {
            throw new ServiceRuntimeException("仅管理员可删除邀请码");
        }
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        if (idList.isEmpty()) {
            throw new ServiceRuntimeException("未选择记录");
        }
        inviteCodeMapper.deleteBatchIds(idList);
        return Result.success("删除成功");
    }

    @Override
    @Transactional
    public void validateAndConsumeForRegister(String code, Integer roleId) {
        if (StringUtils.isBlank(code)) {
            throw new ServiceRuntimeException("教师或管理员注册须填写邀请码");
        }
        String trimmed = code.trim().toUpperCase();
        InviteCode row = inviteCodeMapper.selectOne(
                new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getCode, trimmed));
        if (row == null) {
            throw new ServiceRuntimeException("邀请码无效");
        }
        if (!Objects.equals(row.getRoleId(), roleId)) {
            throw new ServiceRuntimeException("邀请码与所选身份不匹配");
        }
        if (row.getStatus() == null || row.getStatus() != STATUS_ENABLED) {
            throw new ServiceRuntimeException("邀请码已禁用");
        }
        if (row.getExpireTime() != null && row.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceRuntimeException("邀请码已过期");
        }
        if (row.getUsedCount() != null && row.getMaxUses() != null && row.getUsedCount() >= row.getMaxUses()) {
            throw new ServiceRuntimeException("邀请码已用尽");
        }
        int updated = inviteCodeMapper.consumeOnce(row.getId());
        if (updated < 1) {
            throw new ServiceRuntimeException("邀请码不可用或已被使用完");
        }
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            Long cnt = inviteCodeMapper.selectCount(
                    new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getCode, code));
            if (cnt == null || cnt == 0) {
                return code;
            }
        }
        throw new ServiceRuntimeException("生成邀请码失败，请重试");
    }

    private InviteCodeVO toVo(InviteCode row) {
        InviteCodeVO vo = new InviteCodeVO();
        vo.setId(row.getId());
        vo.setCode(row.getCode());
        vo.setRoleId(row.getRoleId());
        vo.setRoleName(roleIdToName(row.getRoleId()));
        vo.setMaxUses(row.getMaxUses());
        vo.setUsedCount(row.getUsedCount());
        vo.setStatus(row.getStatus());
        vo.setRemark(row.getRemark());
        vo.setExpireTime(row.getExpireTime());
        vo.setCreateTime(row.getCreateTime());
        if (row.getCreatorId() != null) {
            User creator = userMapper.selectById(row.getCreatorId());
            if (creator != null) {
                vo.setCreatorName(creator.getRealName());
            }
        }
        return vo;
    }

    private static String roleIdToName(Integer roleId) {
        if (roleId == null) {
            return "";
        }
        if (roleId == 2) {
            return "教师";
        }
        if (roleId == 3) {
            return "管理员";
        }
        return "未知";
    }
}
