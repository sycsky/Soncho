package com.example.aikef.controller;

import com.example.aikef.dto.Result;
import com.example.aikef.dto.ScheduledTaskDto;
import com.example.aikef.dto.request.SaveScheduledTaskRequest;
import com.example.aikef.service.AiScheduledTaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduled-tasks")
public class AiScheduledTaskController {

    @Autowired
    private AiScheduledTaskService scheduledTaskService;

    @PostMapping
    public Result<ScheduledTaskDto> createTask(@Valid @RequestBody SaveScheduledTaskRequest request) {
        return Result.success(scheduledTaskService.createTask(request));
    }

    @PutMapping("/{id}")
    public Result<ScheduledTaskDto> updateTask(@PathVariable UUID id, @Valid @RequestBody SaveScheduledTaskRequest request) {
        return Result.success(scheduledTaskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable UUID id) {
        scheduledTaskService.deleteTask(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ScheduledTaskDto> getTask(@PathVariable UUID id) {
        return Result.success(scheduledTaskService.getTask(id));
    }

    @GetMapping
    public Result<List<ScheduledTaskDto>> getAllTasks() {
        return Result.success(scheduledTaskService.getAllTasks());
    }
}
