package com.example.aikef.dto;

import com.example.aikef.model.enums.SenderType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天消息 DTO
 * 包含客服可见的隐藏元数据
 */
public record ChatMessageDto(
        UUID id,
        UUID sessionId,
        SenderType senderType,
        UUID agentId,
        String agentName,
        String text,
        boolean internal,
        boolean isMine,              // 是否是本人发送的
        Map<String, Object> translationData,
        List<String> mentionAgentIds,
        List<AttachmentDto> attachments,
        Map<String, Object> agentMetadata,  // 客服可见的隐藏元数据（对客户隐藏）
        Instant createdAt
) {
}
