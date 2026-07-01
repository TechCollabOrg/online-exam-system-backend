package cn.org.alan.exam.service;

/**
 * 监考告警 WebSocket 推送（推送给考试创建教师）。
 */
public interface ProctorNotifyService {

    void notifyProctorAlert(Integer examId, Integer studentUserId, String eventType, String detail);
}
