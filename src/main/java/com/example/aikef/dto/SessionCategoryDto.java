package com.example.aikef.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 会话分类 DTO
 */
public record SessionCategoryDto(
        UUID id,
        String name,
        String description,
        String icon,
        String color,
        Integer sortOrder,
        boolean enabled,
        UUID createdByAgentId,
        Instant createdAt,
        Instant updatedAt
) {
}

