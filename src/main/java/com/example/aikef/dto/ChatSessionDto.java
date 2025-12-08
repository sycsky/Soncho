package com.example.aikef.dto;

import com.example.aikef.model.enums.SessionStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天会话 DTO
 */
public record ChatSessionDto(
        UUID id,
        UUID userId,
        CustomerDto user,
        SessionStatus status,
        long lastActive,
        int unreadCount,
        UUID sessionGroupId,
        UUID primaryAgentId,  // 主要客服ID
        List<SessionAgentDto> agents,  // 会话客服列表（主要客服 + 支持客服）
        SessionMessageDto lastMessage,
        String note,
        UUID categoryId,  // 会话分类ID
        SessionCategoryDto category, // 会话分类详情
        Map<String, Object> metadata,  // 会话元数据
        String customerLanguage  // 客户使用的语言代码（如 zh-TW, en, ja）
) {
}
