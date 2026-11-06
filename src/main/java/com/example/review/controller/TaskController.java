package com.example.review.controller;

import com.example.review.domain.Task;
import com.example.review.mapper.TaskMapper;
import com.example.review.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.review.domain.TaskStatus;
import com.example.review.dto.TaskCreateRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskMapper taskMapper;
    private final TaskService taskService;

    public TaskController(TaskMapper taskMapper,TaskService taskService) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> list() {
        return taskMapper.selectList(null);
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody TaskCreateRequest req) {
        Task task = new Task();
        task.setContentId(req.getContentId());
        task.setContentType(req.getContentType());
        task.setPriority(req.getPriority() == null ? 2 : req.getPriority());
        task.setStatus(TaskStatus.PENDING.name());
        task.setVersion(0);

        try {
            taskMapper.insert(task);
            return Map.of("id", task.getId(), "duplicated", false);
        } catch (DuplicateKeyException e) {
            Task exist = taskMapper.selectOne(new LambdaQueryWrapper<Task>()
                    .eq(Task::getContentId, req.getContentId())
                    .eq(Task::getContentType, req.getContentType()));
            return Map.of("id", exist.getId(), "duplicated", true);
        }
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<Object> claim(@PathVariable Long id,
                                        @RequestBody Map<String, Long> body) {
        Long operatorId = body.get("operatorId");
        Task task = taskService.claim(id, operatorId);
        if (task == null) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "任务不存在、状态不允许或已被他人领取"));
        }
        return ResponseEntity.ok(Map.of(
                "id", task.getId(),
                "status", task.getStatus(),
                "assigneeId", task.getAssigneeId(),
                "version", task.getVersion()));
    }
}
