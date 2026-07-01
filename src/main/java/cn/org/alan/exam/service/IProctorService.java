package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.proctor.ProctorEventForm;
import cn.org.alan.exam.model.form.proctor.ProctorLeaveForm;
import cn.org.alan.exam.model.vo.proctor.ProctorConfigVO;
import cn.org.alan.exam.model.vo.proctor.ProctorEventVO;
import cn.org.alan.exam.model.vo.proctor.ProctorParticipantVO;
import cn.org.alan.exam.model.vo.proctor.ProctorTokenVO;

import java.util.List;

public interface IProctorService {

    Result<ProctorConfigVO> getConfig(Integer examId);

    Result<ProctorTokenVO> getToken(Integer examId, String role);

    Result<String> reportEvent(ProctorEventForm form);

    Result<String> heartbeat(Integer examId);

    Result<List<ProctorEventVO>> listEvents(Integer examId, Integer limit);

    Result<List<ProctorParticipantVO>> listParticipants(Integer examId);

    Result<String> requestLeave(ProctorLeaveForm form);

    Result<String> returnLeave(Integer examId);

    void expireOverdueLeaves();
}
