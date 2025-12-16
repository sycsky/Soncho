package com.example.aikef.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 工作流测试会话 DTO
 */
public record WorkflowTestSessionDto(
        String testSessionId,
        UUID workflowId,
        String workflowName,
        List<TestMessage> messages,
        Instant createdAt,
        Instant lastActiveAt
) {
    
    /**
     * 测试消息
     */
    public record TestMessage(
            String id,
            String role,      // user, assistant, system
            String content,
            Instant timestamp,
            TestMessageMeta meta
    ) {}
    
    /**
     * 消息元数据
     */
    public record TestMessageMeta(
            Boolean success,
            Long durationMs,
            String errorMessage,
            Boolean needHumanTransfer,
            List<NodeDetail> nodeDetails
    ) {}
    
    /**
     * 节点执行详情
     */
    public record NodeDetail(
            String nodeId,
            String nodeType,
            String input,
            String output,
            Long durationMs,
            Boolean success
    ) {}
}

