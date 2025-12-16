package com.example.aikef.controller;

import com.example.aikef.dto.WorkflowTestSessionDto;
import com.example.aikef.workflow.service.WorkflowTestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流测试 API 控制器
 * 提供前端对话框测试工作流的接口
 */
@RestController
@RequestMapping("/api/v1/workflow-test")
public class WorkflowTestController {

    private final WorkflowTestService testService;

    public WorkflowTestController(WorkflowTestService testService) {
        this.testService = testService;
    }

    /**
     * 创建测试会话
     */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowTestSessionDto createSession(@Valid @RequestBody CreateTestSessionRequest request) {
        return testService.createTestSession(request.workflowId(), request.variables());
    }

    /**
     * 发送测试消息
     */
    @PostMapping("/sessions/{testSessionId}/messages")
    public WorkflowTestSessionDto sendMessage(
            @PathVariable String testSessionId,
            @Valid @RequestBody SendTestMessageRequest request) {
        return testService.sendTestMessage(testSessionId, request.message());
    }

    /**
     * 获取测试会话
     */
    @GetMapping("/sessions/{testSessionId}")
    public WorkflowTestSessionDto getSession(@PathVariable String testSessionId) {
        return testService.getTestSession(testSessionId);
    }

    /**
     * 清除测试会话历史
     */
    @PostMapping("/sessions/{testSessionId}/clear")
    public WorkflowTestSessionDto clearSession(@PathVariable String testSessionId) {
        return testService.clearTestSession(testSessionId);
    }

    /**
     * 删除测试会话
     */
    @DeleteMapping("/sessions/{testSessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable String testSessionId) {
        testService.deleteTestSession(testSessionId);
    }

    /**
     * 设置测试变量
     */
    @PutMapping("/sessions/{testSessionId}/variables")
    public WorkflowTestSessionDto setVariables(
            @PathVariable String testSessionId,
            @RequestBody Map<String, Object> variables) {
        return testService.setTestVariables(testSessionId, variables);
    }

    /**
     * 获取所有测试会话（管理用）
     */
    @GetMapping("/sessions")
    public List<WorkflowTestSessionDto> getAllSessions() {
        return testService.getAllTestSessions();
    }

    // ==================== 请求记录 ====================

    public record CreateTestSessionRequest(
            @NotNull UUID workflowId,
            Map<String, Object> variables
    ) {}

    public record SendTestMessageRequest(
            @NotBlank String message
    ) {}
}

