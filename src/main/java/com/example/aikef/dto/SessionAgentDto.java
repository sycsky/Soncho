package com.example.aikef.dto;

import com.example.aikef.model.enums.AgentStatus;

import java.util.UUID;

/**
 * 会话客服 DTO
 * 包含客服信息和在会话中的角色
 */
public record SessionAgentDto(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        AgentStatus status,
        UUID roleId,
        String roleName,
        boolean isPrimary  // 是否是主要客服
) {
    /**
     * 从 AgentDto 创建（主要客服）
     */
    public static SessionAgentDto fromAgentDto(AgentDto agentDto, boolean isPrimary) {
        return new SessionAgentDto(
                agentDto.id(),
                agentDto.name(),
                agentDto.email(),
                agentDto.avatarUrl(),
                agentDto.status(),
                agentDto.roleId(),
                agentDto.roleName(),
                isPrimary
        );
    }
}

