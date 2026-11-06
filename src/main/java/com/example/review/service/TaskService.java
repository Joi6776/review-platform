package com.example.review.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.review.domain.Task;
import com.example.review.domain.TaskAction;
import com.example.review.domain.TaskStatus;
import com.example.review.mapper.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    /** 延迟队列的 key：分数 = 到期时间戳（秒），成员 = 任务 id */
    private static final String DELAY_KEY = "review:dispatch:delay";

    private final TaskMapper taskMapper;
    private final StringRedisTemplate redis;
    private final long claimTtlSeconds;

    public TaskService(TaskMapper taskMapper,
                       StringRedisTemplate redis,
                       @Value("${review.claim-ttl-seconds:300}") long claimTtlSeconds) {
        this.taskMapper = taskMapper;
        this.redis = redis;
        this.claimTtlSeconds = claimTtlSeconds;
    }

    public Task claim(Long taskId, Long operatorId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }

        TaskStatus current = TaskStatus.valueOf(task.getStatus());
        if (!TaskStateMachine.canApply(current, TaskAction.CLAIM)) {
            return null;
        }

        LocalDateTime deadline = LocalDateTime.now().plusSeconds(claimTtlSeconds);

        int rows = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, current.name())
                .eq(Task::getVersion, task.getVersion())
                .set(Task::getStatus, TaskStatus.REVIEWING.name())
                .set(Task::getAssigneeId, operatorId)
                .set(Task::getVersion, task.getVersion() + 1)
                .set(Task::getDeadlineAt, deadline));

        if (rows == 0) {
            return null;
        }

        // 领取成功后挂一个闹钟：TTL 秒后如果还没处理，就该被回收
        long expireAt = System.currentTimeMillis() / 1000 + claimTtlSeconds;
        redis.opsForZSet().add(DELAY_KEY, String.valueOf(taskId), expireAt);

        task.setStatus(TaskStatus.REVIEWING.name());
        task.setAssigneeId(operatorId);
        task.setVersion(task.getVersion() + 1);
        task.setDeadlineAt(deadline);
        return task;
    }

    /** 任务被处置后撤掉超时闹钟，避免之后被误回收 */
    public void cancelDelay(Long taskId) {
        redis.opsForZSet().remove(DELAY_KEY, String.valueOf(taskId));
    }

    /**
     * 把已到期但还没处理的任务回收成待审。
     * @return 回收了几条
     */
    public int reclaimExpired() {
        long now = System.currentTimeMillis() / 1000;
        Set<String> ids = redis.opsForZSet().rangeByScore(DELAY_KEY, 0, now, 0, 100);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int reclaimed = 0;
        for (String idStr : ids) {
            Long taskId = Long.valueOf(idStr);
            Task task = taskMapper.selectById(taskId);

            if (task == null) {
                redis.opsForZSet().remove(DELAY_KEY, idStr);
                continue;
            }
            // 已经审完的，说明这个闹钟是脏数据，清掉
            if (!TaskStatus.REVIEWING.name().equals(task.getStatus())) {
                redis.opsForZSet().remove(DELAY_KEY, idStr);
                continue;
            }

            int rows = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, TaskStatus.REVIEWING.name())
                    .eq(Task::getVersion, task.getVersion())
                    .set(Task::getStatus, TaskStatus.PENDING.name())
                    .set(Task::getVersion, task.getVersion() + 1)
                    .setSql("assignee_id = NULL, deadline_at = NULL"));

            if (rows > 0) {
                redis.opsForZSet().remove(DELAY_KEY, idStr);
                reclaimed++;
                log.info("任务 {} 超时未处理，已回收为待审", taskId);
            }
        }
        return reclaimed;
    }
}