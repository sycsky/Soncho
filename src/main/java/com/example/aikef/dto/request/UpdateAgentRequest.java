package com.example.aikef.dto.request;

import com.example.aikef.model.enums.AgentStatus;
import java.util.UUID;

public record UpdateAgentRequest(
        String name,
        String email,
        AgentStatus status,
        UUID roleId,
        String language  // 客服使用的语言代码（如 zh-CN, en, ja）
) {}
