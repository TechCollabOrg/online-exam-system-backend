package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.invite.InviteCodeForm;
import cn.org.alan.exam.model.vo.invite.InviteCodeVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 注册邀请码：生成、分页、禁用/删除；注册时校验并消耗。
 */
public interface IInviteCodeService {

    Result<String> create(InviteCodeForm form);

    Result<IPage<InviteCodeVO>> paging(Integer pageNum, Integer pageSize, Integer roleId, Integer status);

    Result<String> disable(Integer id);

    Result<String> deleteBatch(String ids);

    /**
     * 注册时校验邀请码并扣减一次使用次数。
     *
     * @param code   用户输入的邀请码
     * @param roleId 注册选择的角色
     */
    void validateAndConsumeForRegister(String code, Integer roleId);
}
