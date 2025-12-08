package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

/**
 * 执行工作流请求
 */
public record ExecuteWorkflowRequest(
    /**
     * 会话ID（可选，用于关联会话）
     */
    UUID sessionId,
    
    /**
     * 用户输入消息
     */
    @NotBlank(message = "用户消息不能为空")
    String userMessage,
    
    /**
     * 额外变量（可选）
     */
    Map<String, Object> variables
) {}

