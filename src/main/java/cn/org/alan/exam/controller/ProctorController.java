package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.proctor.ProctorEventForm;
import cn.org.alan.exam.model.form.proctor.ProctorLeaveForm;
import cn.org.alan.exam.model.vo.proctor.ProctorConfigVO;
import cn.org.alan.exam.model.vo.proctor.ProctorEventVO;
import cn.org.alan.exam.model.vo.proctor.ProctorParticipantVO;
import cn.org.alan.exam.model.vo.proctor.ProctorTokenVO;
import cn.org.alan.exam.service.IProctorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "考试监考")
@RestController
@RequestMapping("/api/proctor")
public class ProctorController {

    @Resource
    private IProctorService proctorService;

    @ApiOperation("监考配置（学生/教师）")
    @GetMapping("/config/{examId}")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<ProctorConfigVO> config(@PathVariable Integer examId) {
        return proctorService.getConfig(examId);
    }

    @ApiOperation("LiveKit 连接 Token")
    @GetMapping("/token")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<ProctorTokenVO> token(@RequestParam Integer examId,
                                        @RequestParam String role) {
        return proctorService.getToken(examId, role);
    }

    @ApiOperation("上报监考事件（学生端人脸/摄像头）")
    @PostMapping("/events")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<String> reportEvent(@Validated @RequestBody ProctorEventForm form) {
        return proctorService.reportEvent(form);
    }

    @ApiOperation("在线心跳")
    @PostMapping("/heartbeat")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<String> heartbeat(@RequestParam Integer examId) {
        return proctorService.heartbeat(examId);
    }

    @ApiOperation("告警列表（教师）")
    @GetMapping("/events")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<ProctorEventVO>> listEvents(@RequestParam Integer examId,
                                                   @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return proctorService.listEvents(examId, limit);
    }

    @ApiOperation("进行中考生（教师）")
    @GetMapping("/participants")
    @PreAuthorize("hasAnyAuthority('role_teacher','role_admin')")
    public Result<List<ProctorParticipantVO>> participants(@RequestParam Integer examId) {
        return proctorService.listParticipants(examId);
    }

    @ApiOperation("申请暂离")
    @PostMapping("/leave")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<String> leave(@Validated @RequestBody ProctorLeaveForm form) {
        return proctorService.requestLeave(form);
    }

    @ApiOperation("暂离返回")
    @PostMapping("/leave/return")
    @PreAuthorize("hasAnyAuthority('role_student')")
    public Result<String> returnLeave(@RequestParam Integer examId) {
        return proctorService.returnLeave(examId);
    }
}
