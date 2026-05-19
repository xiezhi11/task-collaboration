package com.taskcollab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taskcollab.common.Result;
import com.taskcollab.dto.TaskAcceptDTO;
import com.taskcollab.dto.TaskCreateDTO;
import com.taskcollab.dto.TaskProgressDTO;
import com.taskcollab.dto.TaskQueryDTO;
import com.taskcollab.dto.TaskUpdateDTO;
import com.taskcollab.entity.Task;
import com.taskcollab.entity.TaskOperationLog;
import com.taskcollab.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@Validated
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping
    public Result<Task> createTask(@Valid @RequestBody TaskCreateDTO dto) {
        return taskService.createTask(dto);
    }

    @PostMapping("/{taskId}/publish")
    public Result<Task> publishTask(@PathVariable Long taskId) {
        return taskService.publishTask(taskId);
    }

    @PostMapping("/{taskId}/assign")
    public Result<Task> assignTask(@PathVariable Long taskId, @RequestParam Long executorId) {
        return taskService.assignTask(taskId, executorId);
    }

    @PostMapping("/progress")
    public Result<Task> updateProgress(@Valid @RequestBody TaskProgressDTO dto) {
        return taskService.updateProgress(dto);
    }

    @PostMapping("/{taskId}/submit-accept")
    public Result<Task> submitAccept(@PathVariable Long taskId) {
        return taskService.submitAccept(taskId);
    }

    @PostMapping("/{taskId}/accept-pass")
    public Result<Task> acceptPass(@PathVariable Long taskId) {
        return taskService.acceptPass(taskId);
    }

    @PostMapping("/accept-reject")
    public Result<Task> acceptReject(@Valid @RequestBody TaskAcceptDTO dto) {
        return taskService.acceptReject(dto);
    }

    @PutMapping("/{taskId}")
    public Result<Task> updateTask(@PathVariable Long taskId, @RequestBody TaskUpdateDTO dto) {
        return taskService.updateTask(taskId, dto);
    }

    @GetMapping
    public Result<Page<Task>> queryTasks(TaskQueryDTO dto) {
        return taskService.queryTasks(dto);
    }

    @GetMapping("/{taskId}")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable Long taskId) {
        return taskService.getTaskDetail(taskId);
    }

    @GetMapping("/kanban")
    public Result<Map<String, List<Task>>> getKanban(TaskQueryDTO dto) {
        return taskService.getKanban(dto);
    }

    @GetMapping("/{taskId}/logs")
    public Result<List<TaskOperationLog>> getOperationLogs(@PathVariable Long taskId) {
        return taskService.getOperationLogs(taskId);
    }
}
