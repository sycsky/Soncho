package com.example.aikef.dto;

import java.util.UUID;

/**
 * 知识库简要信息 DTO（用于 Bootstrap）
 */
public record KnowledgeEntryDto(
        UUID id,
        String name,
        String description,
        Integer documentCount,
        Boolean enabled) {
}
