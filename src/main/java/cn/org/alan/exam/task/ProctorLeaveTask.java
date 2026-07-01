package cn.org.alan.exam.task;

import cn.org.alan.exam.service.IProctorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 每 30 秒扫描暂离超时记录并写入告警。
 */
@Component
@Slf4j
public class ProctorLeaveTask {

    @Resource
    private IProctorService proctorService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void checkLeaveTimeout() {
        try {
            proctorService.expireOverdueLeaves();
        } catch (Exception e) {
            log.warn("监考暂离超时扫描异常: {}", e.getMessage());
        }
    }
}
