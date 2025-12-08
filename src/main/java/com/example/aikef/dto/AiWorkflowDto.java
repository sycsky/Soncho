package com.example.aikef.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AI 工作流 DTO
 */
public record AiWorkflowDto(
    UUID id,
    String name,
    String description,
    String nodesJson,
    String edgesJson,
    String liteflowEl,
    Integer version,
    Boolean enabled,
    Boolean isDefault,
    UUID createdByAgentId,
    String createdByAgentName,
    String triggerType,
    String triggerConfig,
    List<UUID> categoryIds,          // 绑定的分类ID列表
    List<CategoryInfo> categories,   // 绑定的分类详情
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * 分类简要信息
     */
    public record CategoryInfo(
        UUID id,
        String name,
        String color,
        String icon
    ) {}
}

