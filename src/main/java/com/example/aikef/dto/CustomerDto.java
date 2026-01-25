package com.example.aikef.dto;

import com.example.aikef.model.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CustomerDto(
        UUID id,
        String name,
        Channel primaryChannel,
        String email,
        String phone,
        String wechatOpenId,
        String whatsappId,
        String lineId,
        String telegramId,
        String facebookId,
        String avatarUrl,
        String location,
        String notes,
        @JsonProperty("metadata") Map<String, Object> customFields,
        String shopifyCustomerId,
        Map<String, Object> shopifyCustomerInfo,
        boolean active,
        Instant lastInteractionAt,
        Instant createdAt,
        List<String> tags,      // 手动添加的标签
        List<String> aiTags,     // AI生成的标签
        String roleCode,        // 客户角色代码 (SUPPLIER, LOGISTICS, etc.)
        String roleName         // 客户角色名称 (供货商, 物流人员, etc.)
) {
}
