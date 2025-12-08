package com.example.aikef.dto;

import com.example.aikef.model.enums.AgentStatus;
import java.util.UUID;

public record AgentDto(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        AgentStatus status,
        UUID roleId,
        String roleName,
        String language) {  // 客服使用的语言代码
}
