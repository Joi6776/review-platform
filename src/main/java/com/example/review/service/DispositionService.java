package com.example.review.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.review.domain.Task;
import com.example.review.domain.TaskAction;
import com.example.review.domain.TaskStatus;
import com.example.review.dto.DisposeRequest;
import com.example.review.mapper.DisposalMapper;
import com.example.review.mapper.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DispositionService {

    private static final Logger log = LoggerFactory.getLogger(DispositionService.class);

    private final TaskMapper taskMapper;
    private final DisposalMapper disposalMapper;
    private final TaskService taskService;

    public DispositionService(TaskMapper taskMapper,
                              DisposalMapper disposalMapper,
                              TaskService taskService) {
        this.taskMapper = taskMapper;
        this.disposalMapper = disposalMapper;
        this.taskService = taskService;
    }

    /**
     * 处置一条任务：改状态 + 建案件 + 记处置 + 写审计，四步在一个事务里。
     */
    @Transactional
    public Map<String, Object> dispose(Long taskId, Long operatorId, DisposeRequest req) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在：" + taskId);
        }

        TaskStatus current = TaskStatus.valueOf(task.getStatus());
        if (!TaskStateMachine.canApply(current, TaskAction.DISPOSE)) {
            throw new IllegalStateException("当前状态 " + current + " 不允许处置");
        }

        int rows = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, current.name())
                .eq(Task::getVersion, task.getVersion())
                .set(Task::getStatus, TaskStatus.DISPOSED.name())
                .set(Task::getVersion, task.getVersion() + 1));

        if (rows == 0) {
            throw new IllegalStateException("并发冲突，任务已被其他人修改");
        }

        disposalMapper.insertCase(taskId, req.getViolationType());
        Long caseId = disposalMapper.lastInsertId();

        disposalMapper.insertDisposition(caseId, req.getAction(), operatorId,
                req.getReviewerId(), req.getReason());
        Long dispositionId = disposalMapper.lastInsertId();

        String before = "{\"status\":\"" + current.name() + "\"}";
        String after = "{\"status\":\"DISPOSED\",\"action\":\"" + req.getAction()
                + "\",\"caseId\":" + caseId + ",\"dispositionId\":" + dispositionId + "}";

        disposalMapper.insertAudit(operatorId, "DISPOSE", "TASK", taskId, before, after);

        taskService.cancelDelay(taskId);

        log.info("任务 {} 已处置：action={} caseId={} operator={}",
                taskId, req.getAction(), caseId, operatorId);

        return Map.of(
                "taskId", taskId,
                "status", TaskStatus.DISPOSED.name(),
                "caseId", caseId,
                "dispositionId", dispositionId);
    }
}