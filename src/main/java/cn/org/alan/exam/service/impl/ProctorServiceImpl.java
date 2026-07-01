package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.config.LiveKitProperties;
import cn.org.alan.exam.mapper.*;
import cn.org.alan.exam.model.entity.*;
import cn.org.alan.exam.model.form.proctor.ProctorEventForm;
import cn.org.alan.exam.model.form.proctor.ProctorLeaveForm;
import cn.org.alan.exam.model.vo.proctor.*;
import cn.org.alan.exam.service.IProctorService;
import cn.org.alan.exam.service.ProctorNotifyService;
import cn.org.alan.exam.utils.LiveKitTokenUtil;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProctorServiceImpl implements IProctorService {

    private static final int PRESENCE_ONLINE_SEC = 90;

    @Resource
    private ExamMapper examMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private ProctorEventMapper proctorEventMapper;
    @Resource
    private ProctorLeaveMapper proctorLeaveMapper;
    @Resource
    private ProctorPresenceMapper proctorPresenceMapper;
    @Resource
    private LiveKitProperties liveKitProperties;
    @Resource
    private LiveKitTokenUtil liveKitTokenUtil;
    @Resource
    private ProctorNotifyService proctorNotifyService;

    @Override
    public Result<ProctorConfigVO> getConfig(Integer examId) {
        Exam exam = requireExam(examId);
        ProctorConfigVO vo = new ProctorConfigVO();
        vo.setProctorEnabled(exam.getProctorEnabled());
        vo.setAllowLeave(exam.getAllowLeave());
        vo.setLeaveMaxMinutes(exam.getLeaveMaxMinutes());
        vo.setLeaveMaxCount(exam.getLeaveMaxCount());
        return Result.success("", vo);
    }

    @Override
    public Result<ProctorTokenVO> getToken(Integer examId, String role) {
        Exam exam = requireExam(examId);
        if (exam.getProctorEnabled() == null || exam.getProctorEnabled() != 1) {
            throw new ServiceRuntimeException("本场考试未启用监考");
        }
        if (!liveKitProperties.isEnabled()) {
            throw new ServiceRuntimeException("LiveKit 未启用，请联系管理员");
        }
        Integer userId = SecurityUtil.getUserId();
        String normalizedRole = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        String identity;
        boolean canPublish;
        boolean canSubscribe;
        if ("student".equals(normalizedRole)) {
            assertStudentOngoing(examId, userId);
            identity = LiveKitTokenUtil.studentIdentity(userId);
            canPublish = true;
            canSubscribe = false;
        } else if ("proctor".equals(normalizedRole)) {
            assertProctorAccess(exam, userId);
            identity = LiveKitTokenUtil.proctorIdentity(userId);
            canPublish = false;
            canSubscribe = true;
        } else {
            throw new ServiceRuntimeException("role 须为 student 或 proctor");
        }
        ProctorTokenVO vo = new ProctorTokenVO();
        vo.setLivekitUrl(liveKitProperties.getUrl());
        vo.setRoomName(LiveKitTokenUtil.roomName(examId));
        vo.setIdentity(identity);
        vo.setToken(liveKitTokenUtil.createToken(identity, examId, canPublish, canSubscribe));
        return Result.success("", vo);
    }

    @Override
    @Transactional
    public Result<String> reportEvent(ProctorEventForm form) {
        Exam exam = requireExam(form.getExamId());
        if (exam.getProctorEnabled() == null || exam.getProctorEnabled() != 1) {
            return Result.success("ok");
        }
        Integer userId = SecurityUtil.getUserId();
        ProctorEvent row = new ProctorEvent();
        row.setExamId(form.getExamId());
        row.setUserId(userId);
        row.setEventType(form.getEventType());
        row.setDetail(form.getDetail());
        row.setCreateTime(LocalDateTime.now());
        proctorEventMapper.insert(row);
        proctorNotifyService.notifyProctorAlert(form.getExamId(), userId, form.getEventType(), form.getDetail());
        return Result.success("ok");
    }

    @Override
    public Result<String> heartbeat(Integer examId) {
        requireExam(examId);
        Integer userId = SecurityUtil.getUserId();
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ProctorPresence> q = new LambdaQueryWrapper<>();
        q.eq(ProctorPresence::getExamId, examId).eq(ProctorPresence::getUserId, userId);
        ProctorPresence existing = proctorPresenceMapper.selectOne(q);
        if (existing == null) {
            ProctorPresence row = new ProctorPresence();
            row.setExamId(examId);
            row.setUserId(userId);
            row.setLastHeartbeat(now);
            proctorPresenceMapper.insert(row);
        } else {
            existing.setLastHeartbeat(now);
            proctorPresenceMapper.updateById(existing);
        }
        return Result.success("ok");
    }

    @Override
    public Result<List<ProctorEventVO>> listEvents(Integer examId, Integer limit) {
        Exam exam = requireExam(examId);
        assertProctorAccess(exam, SecurityUtil.getUserId());
        int lim = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        LambdaQueryWrapper<ProctorEvent> q = new LambdaQueryWrapper<>();
        q.eq(ProctorEvent::getExamId, examId)
                .orderByDesc(ProctorEvent::getCreateTime)
                .last("LIMIT " + lim);
        List<ProctorEvent> rows = proctorEventMapper.selectList(q);
        Map<Integer, User> userMap = loadUsers(rows.stream().map(ProctorEvent::getUserId).collect(Collectors.toSet()));
        List<ProctorEventVO> list = new ArrayList<>();
        for (ProctorEvent e : rows) {
            ProctorEventVO vo = new ProctorEventVO();
            vo.setId(e.getId());
            vo.setExamId(e.getExamId());
            vo.setUserId(e.getUserId());
            User u = userMap.get(e.getUserId());
            if (u != null) {
                vo.setUserName(u.getRealName() != null ? u.getRealName() : u.getUserName());
            }
            vo.setEventType(e.getEventType());
            vo.setDetail(e.getDetail());
            vo.setCreateTime(e.getCreateTime());
            list.add(vo);
        }
        return Result.success("", list);
    }

    @Override
    public Result<List<ProctorParticipantVO>> listParticipants(Integer examId) {
        Exam exam = requireExam(examId);
        assertProctorAccess(exam, SecurityUtil.getUserId());
        LambdaQueryWrapper<UserExamsScore> scoreQ = new LambdaQueryWrapper<>();
        scoreQ.eq(UserExamsScore::getExamId, examId).eq(UserExamsScore::getState, 0);
        List<UserExamsScore> ongoing = userExamsScoreMapper.selectList(scoreQ);
        if (ongoing.isEmpty()) {
            return Result.success("", Collections.emptyList());
        }
        Set<Integer> userIds = ongoing.stream().map(UserExamsScore::getUserId).collect(Collectors.toSet());
        Map<Integer, User> userMap = loadUsers(userIds);
        LocalDateTime onlineThreshold = LocalDateTime.now().minusSeconds(PRESENCE_ONLINE_SEC);
        LambdaQueryWrapper<ProctorPresence> presenceQ = new LambdaQueryWrapper<>();
        presenceQ.eq(ProctorPresence::getExamId, examId).in(ProctorPresence::getUserId, userIds);
        Map<Integer, ProctorPresence> presenceMap = proctorPresenceMapper.selectList(presenceQ).stream()
                .collect(Collectors.toMap(ProctorPresence::getUserId, p -> p, (a, b) -> a));
        LambdaQueryWrapper<ProctorLeave> leaveQ = new LambdaQueryWrapper<>();
        leaveQ.eq(ProctorLeave::getExamId, examId).eq(ProctorLeave::getStatus, 0).in(ProctorLeave::getUserId, userIds);
        Set<Integer> inLeaveIds = proctorLeaveMapper.selectList(leaveQ).stream()
                .map(ProctorLeave::getUserId).collect(Collectors.toSet());
        List<ProctorParticipantVO> list = new ArrayList<>();
        for (UserExamsScore s : ongoing) {
            ProctorParticipantVO vo = new ProctorParticipantVO();
            vo.setUserId(s.getUserId());
            User u = userMap.get(s.getUserId());
            if (u != null) {
                vo.setUserName(u.getUserName());
                vo.setRealName(u.getRealName());
            }
            vo.setLivekitIdentity(LiveKitTokenUtil.studentIdentity(s.getUserId()));
            ProctorPresence p = presenceMap.get(s.getUserId());
            vo.setOnline(p != null && p.getLastHeartbeat() != null && p.getLastHeartbeat().isAfter(onlineThreshold));
            vo.setInLeave(inLeaveIds.contains(s.getUserId()));
            list.add(vo);
        }
        return Result.success("", list);
    }

    @Override
    @Transactional
    public Result<String> requestLeave(ProctorLeaveForm form) {
        Exam exam = requireExam(form.getExamId());
        if (exam.getAllowLeave() == null || exam.getAllowLeave() != 1) {
            throw new ServiceRuntimeException("本场考试不允许暂离");
        }
        Integer userId = SecurityUtil.getUserId();
        assertStudentOngoing(form.getExamId(), userId);
        int maxMin = exam.getLeaveMaxMinutes() != null ? exam.getLeaveMaxMinutes() : 5;
        if (form.getMinutes() > maxMin) {
            throw new ServiceRuntimeException("单次暂离不能超过 " + maxMin + " 分钟");
        }
        int maxCount = exam.getLeaveMaxCount() != null ? exam.getLeaveMaxCount() : 1;
        LambdaQueryWrapper<ProctorLeave> countQ = new LambdaQueryWrapper<>();
        countQ.eq(ProctorLeave::getExamId, form.getExamId()).eq(ProctorLeave::getUserId, userId);
        long used = proctorLeaveMapper.selectCount(countQ);
        if (used >= maxCount) {
            throw new ServiceRuntimeException("暂离次数已用完");
        }
        LambdaQueryWrapper<ProctorLeave> activeQ = new LambdaQueryWrapper<>();
        activeQ.eq(ProctorLeave::getExamId, form.getExamId())
                .eq(ProctorLeave::getUserId, userId)
                .eq(ProctorLeave::getStatus, 0);
        if (proctorLeaveMapper.selectCount(activeQ) > 0) {
            throw new ServiceRuntimeException("当前已在暂离中");
        }
        LocalDateTime now = LocalDateTime.now();
        ProctorLeave leave = new ProctorLeave();
        leave.setExamId(form.getExamId());
        leave.setUserId(userId);
        leave.setMinutes(form.getMinutes());
        leave.setStartTime(now);
        leave.setExpectedEnd(now.plusMinutes(form.getMinutes()));
        leave.setStatus(0);
        leave.setCreateTime(now);
        proctorLeaveMapper.insert(leave);
        saveAndNotifyEvent(form.getExamId(), userId, "LEAVE_START", "暂离 " + form.getMinutes() + " 分钟");
        return Result.success("ok");
    }

    @Override
    @Transactional
    public Result<String> returnLeave(Integer examId) {
        requireExam(examId);
        Integer userId = SecurityUtil.getUserId();
        LambdaQueryWrapper<ProctorLeave> q = new LambdaQueryWrapper<>();
        q.eq(ProctorLeave::getExamId, examId)
                .eq(ProctorLeave::getUserId, userId)
                .eq(ProctorLeave::getStatus, 0)
                .orderByDesc(ProctorLeave::getId)
                .last("LIMIT 1");
        ProctorLeave leave = proctorLeaveMapper.selectOne(q);
        if (leave == null) {
            throw new ServiceRuntimeException("没有进行中的暂离记录");
        }
        leave.setStatus(1);
        leave.setActualEnd(LocalDateTime.now());
        proctorLeaveMapper.updateById(leave);
        saveAndNotifyEvent(examId, userId, "LEAVE_RETURN", "考生已返回");
        return Result.success("ok");
    }

    @Override
    @Transactional
    public void expireOverdueLeaves() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ProctorLeave> q = new LambdaQueryWrapper<>();
        q.eq(ProctorLeave::getStatus, 0).lt(ProctorLeave::getExpectedEnd, now);
        List<ProctorLeave> overdue = proctorLeaveMapper.selectList(q);
        for (ProctorLeave leave : overdue) {
            leave.setStatus(2);
            leave.setActualEnd(now);
            proctorLeaveMapper.updateById(leave);
            saveAndNotifyEvent(leave.getExamId(), leave.getUserId(), "LEAVE_EXPIRED", "暂离超时未返回");
        }
    }

    private void saveAndNotifyEvent(Integer examId, Integer userId, String eventType, String detail) {
        ProctorEvent row = new ProctorEvent();
        row.setExamId(examId);
        row.setUserId(userId);
        row.setEventType(eventType);
        row.setDetail(detail);
        row.setCreateTime(LocalDateTime.now());
        proctorEventMapper.insert(row);
        proctorNotifyService.notifyProctorAlert(examId, userId, eventType, detail);
    }

    private Exam requireExam(Integer examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ServiceRuntimeException("考试不存在");
        }
        return exam;
    }

    private void assertStudentOngoing(Integer examId, Integer userId) {
        LambdaQueryWrapper<UserExamsScore> q = new LambdaQueryWrapper<>();
        q.eq(UserExamsScore::getExamId, examId)
                .eq(UserExamsScore::getUserId, userId)
                .eq(UserExamsScore::getState, 0);
        if (userExamsScoreMapper.selectCount(q) == 0) {
            throw new ServiceRuntimeException("未找到进行中的考试记录");
        }
    }

    private void assertProctorAccess(Exam exam, Integer userId) {
        Integer roleCode = SecurityUtil.getRoleCode();
        if (roleCode == 3) {
            return;
        }
        if (roleCode == 2 && exam.getUserId() != null && exam.getUserId().equals(userId)) {
            return;
        }
        throw new ServiceRuntimeException("无权查看本场监考");
    }

    private Map<Integer, User> loadUsers(Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }
}
