package com.example.aikef.dto;

import java.util.List;

public record BootstrapResponse(
        List<SessionGroupDto> sessionGroups,
        AgentDto agent,
        List<RoleDto> roles,
        List<QuickReplyDto> quickReplies,
        List<KnowledgeEntryDto> knowledgeBase) {
}
