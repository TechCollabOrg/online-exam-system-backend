package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.ExamMapper;
import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.service.ProctorNotifyService;
import cn.org.alan.exam.websocket.WebsocketHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ProctorNotifyServiceImpl implements ProctorNotifyService {

    @Resource
    private ExamMapper examMapper;

    @Override
    public void notifyProctorAlert(Integer examId, Integer studentUserId, String eventType, String detail) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null || exam.getUserId() == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "proctor_alert");
        payload.put("examId", examId);
        payload.put("userId", studentUserId);
        payload.put("eventType", eventType);
        payload.put("detail", detail);
        String json = JSON.toJSONString(payload);
        WebsocketHandler.sendToUser(exam.getUserId(), json);
    }
}
