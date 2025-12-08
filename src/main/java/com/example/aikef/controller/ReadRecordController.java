package com.example.aikef.controller;

import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.ReadRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 已读记录管理 API
 * 管理客服对会话的已读状态
 */
@RestController
@RequestMapping("/api/v1/read-records")
public class ReadRecordController {

    private final ReadRecordService readRecordService;

    public ReadRecordController(ReadRecordService readRecordService) {
        this.readRecordService = readRecordService;
    }

    /**
     * 更新已读时间
     * 当客服打开会话消息框时调用
     */
    @PostMapping("/sessions/{sessionId}/mark-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        
        UUID agentId = getAgentId(authentication);
        readRecordService.updateReadTime(sessionId, agentId);
    }

    /**
     * 获取会话的未读消息数
     */
    @GetMapping("/sessions/{sessionId}/unread-count")
    public int getUnreadCount(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        
        UUID agentId = getAgentId(authentication);
        return readRecordService.getUnreadCount(sessionId, agentId);
    }

    /**
     * 获取客服的总未读会话数
     */
    @GetMapping("/unread-sessions-count")
    public long getTotalUnreadSessions(Authentication authentication) {
        UUID agentId = getAgentId(authentication);
        return readRecordService.getTotalUnreadSessions(agentId);
    }

    private UUID getAgentId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("需要客服认证");
    }
}
