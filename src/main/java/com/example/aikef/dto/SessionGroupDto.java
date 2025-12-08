package com.example.aikef.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Session 分组 DTO
 */
public record SessionGroupDto(
        UUID id,
        String name,
        boolean system,
        UUID agentId,
        String icon,
        String color,
        Integer sortOrder,
        List<ChatSessionDto> sessions,  // 该分组下的所有会话
        List<SessionCategoryDto> categories,  // 绑定的分类列表
        Instant createdAt,
        Instant updatedAt
) {
}
