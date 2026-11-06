package com.example.review.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DispatchTimeoutScheduler {
    private final TaskService taskService;

    public DispatchTimeoutScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 每 10 秒扫一次有没有该回收的任务 */
    @Scheduled(fixedDelay = 10000)
    public void reclaim() {
        int n = taskService.reclaimExpired();
        if (n > 0) {
            // 有回收才打日志，避免日志被刷屏
            System.out.println("本轮回收任务数：" + n);
        }
    }
}
