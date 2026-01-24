package com.example.aikef.controller;


import com.example.aikef.dto.AgentDto;
import com.example.aikef.dto.ChatMessageDto;
import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.dto.SessionAgentDto;
import com.example.aikef.dto.request.TransferSessionRequest;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import com.example.aikef.service.ChatSessionService;
import com.example.aikef.service.MessageService;
import com.example.aikef.service.SessionSummaryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天相关 API
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final MessageService messageService;
    private final ChatSessionService chatSessionService;
    private final EntityMapper entityMapper;
    private final SessionSummaryService sessionSummaryService;

    public ChatController(MessageService messageService,
                          ChatSessionService chatSessionService,
                          EntityMapper entityMapper,
                          SessionSummaryService sessionSummaryService) {
        this.messageService = messageService;
        this.chatSessionService = chatSessionService;
        this.entityMapper = entityMapper;
        this.sessionSummaryService = sessionSummaryService;
    }

    /**
     * 获取会话详情
     * 返回与 bootstrap 接口中 sessions 元素相同的数据结构
     * 需要客服认证，只返回该客服视角下的会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public ChatSessionDto getSession(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        
        // 获取当前客服ID
        UUID agentId = null;
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            agentId = ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        
        if (agentId == null) {
            throw new IllegalStateException("需要客服认证");
        }
        
        return chatSessionService.getSessionDto(sessionId, agentId);
    }

    /**
     * 获取会话的历史消息
     * 支持坐席和客户调用
     * - 坐席：可以看到 agentMetadata（隐藏信息）
     * - 客户：看不到 agentMetadata
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Page<ChatMessageDto>getSessionMessages(
            @PathVariable UUID sessionId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        
            AgentPrincipal agentPrincipal = null;
        CustomerPrincipal customerPrincipal = null;
        
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            agentPrincipal = (AgentPrincipal) authentication.getPrincipal();
        } else if (authentication != null && authentication.getPrincipal() instanceof CustomerPrincipal) {
            customerPrincipal = (CustomerPrincipal) authentication.getPrincipal();
        }
        
        Page<ChatMessageDto> messages = messageService.getSessionMessages(
                sessionId,
                agentPrincipal,
                customerPrincipal,
                pageable
        );
        
        return messages;
    }

    // ==================== 支持客服管理 ====================

    /**
     * 获取会话中的其他客服（排除当前登录客服）
     * GET /api/v1/chat/sessions/{sessionId}/other-agents
     * 
     * 返回当前会话的主要客服和支持客服，但排除当前登录的客服自己
     * 
     * @return 其他客服列表，包含 isPrimary 标识
     */
    @GetMapping("/sessions/{sessionId}/other-agents")
    public List<SessionAgentDto> getOtherSessionAgents(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        UUID currentAgentId = requireAgentAuthAndGetId(authentication);
        ChatSession session = chatSessionService.findById(sessionId);
        UUID primaryAgentId = session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null;
        
        return chatSessionService.getOtherSessionAgents(sessionId, currentAgentId)
                .stream()
                .map(agent -> {
                    AgentDto agentDto = entityMapper.toAgentDto(agent);
                    boolean isPrimary = primaryAgentId != null && primaryAgentId.equals(agent.getId());
                    return SessionAgentDto.fromAgentDto(agentDto, isPrimary);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取可分配给会话的客服列表
     * GET /api/v1/chat/sessions/{sessionId}/available-agents
     * 
     * 排除当前会话的主要客服和已有的支持客服
     * 
     * @return 可分配的客服列表
     */
    @GetMapping("/sessions/{sessionId}/available-agents")
    public List<AgentDto> getAvailableAgentsForSession(@PathVariable UUID sessionId) {
        return chatSessionService.getAvailableAgentsForSession(sessionId)
                .stream()
                .map(entityMapper::toAgentDto)
                .collect(Collectors.toList());
    }

    /**
     * 为会话分配支持客服
     * POST /api/v1/chat/sessions/{sessionId}/agents/{agentId}
     */
    @PostMapping("/sessions/{sessionId}/agents/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignSupportAgent(
            @PathVariable UUID sessionId,
            @PathVariable UUID agentId,
            Authentication authentication) {
        requireAgentAuth(authentication);
        chatSessionService.assignSupportAgent(sessionId, agentId);
    }

    /**
     * 移除会话的支持客服
     * DELETE /api/v1/chat/sessions/{sessionId}/agents/{agentId}
     */
    @DeleteMapping("/sessions/{sessionId}/agents/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSupportAgent(
            @PathVariable UUID sessionId,
            @PathVariable UUID agentId,
            Authentication authentication) {
        requireAgentAuth(authentication);
        chatSessionService.removeSupportAgent(sessionId, agentId);
    }

    // ==================== 转移会话功能 ====================

    /**
     * 获取可转移的客服列表
     * GET /api/v1/chat/sessions/{sessionId}/transferable-agents
     * 
     * 返回可以接收该会话转移的客服列表
     * 排除当前会话的主要负责客服
     * 
     * 注意：只有当前会话的主要负责客服可以调用此接口
     * 
     * @param sessionId 会话ID
     * @return 可转移的客服列表
     */
    @GetMapping("/sessions/{sessionId}/transferable-agents")
    public List<AgentDto> getTransferableAgents(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        UUID currentAgentId = requireAgentAuthAndGetId(authentication);
        return chatSessionService.getTransferableAgents(sessionId, currentAgentId)
                .stream()
                .map(entityMapper::toAgentDto)
                .collect(Collectors.toList());
    }

    /**
     * 转移会话到新的主要负责客服
     * POST /api/v1/chat/sessions/{sessionId}/transfer
     * 
     * 将当前会话转移给另一个客服作为主要负责人
     * 
     * 注意：只有当前会话的主要负责客服可以调用此接口
     * 
     * @param sessionId 会话ID
     * @param request 转移请求，包含目标客服ID和是否保留原客服为支持客服
     */
    @PostMapping("/sessions/{sessionId}/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody TransferSessionRequest request,
            Authentication authentication) {
        UUID currentAgentId = requireAgentAuthAndGetId(authentication);
        
        if (request.keepAsSupport() != null && request.keepAsSupport()) {
            chatSessionService.transferSession(sessionId, currentAgentId, 
                    request.targetAgentId(), true);
        } else {
            chatSessionService.transferSession(sessionId, currentAgentId, 
                    request.targetAgentId());
        }
    }

    // ==================== 会话总结和结束功能 ====================

    /**
     * 预览会话总结（不保存）
     * GET /api/v1/chat/sessions/{sessionId}/summary/preview
     * 
     * 在 Resolve 会话前，先预览 AI 生成的会话总结
     * 总结范围：从上一条 SYSTEM 消息之后到当前时间
     * 如果没有 SYSTEM 消息，则总结所有会话内容
     * 
     * @param sessionId 会话ID
     * @param language 语言代码（可选，例如：zh, en）
     * @return 总结预览结果
     */
    @GetMapping("/sessions/{sessionId}/summary/preview")
    public SessionSummaryService.SummaryResult previewSessionSummary(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        requireAgentAuth(authentication);
        return sessionSummaryService.generateSummary(sessionId, language);
    }

    /**
     * 结束会话（Resolve）
     * POST /api/v1/chat/sessions/{sessionId}/resolve
     * 
     * 结束会话并生成 AI 总结，总结会保存为 SYSTEM 类型的消息
     * 
     * 处理逻辑：
     * 1. 获取从上一条 SYSTEM 消息之后到当前的所有对话
     * 2. 调用 AI 生成对话总结
     * 3. 将总结保存为 SYSTEM 类型消息
     * 4. 将会话状态设置为 RESOLVED
     * 
     * 注意：一个会话可能有多次 Resolve（重新打开后再次结束），
     * 每次 Resolve 都会生成新的 SYSTEM 消息作为总结
     * 
     * @param sessionId 会话ID
     * @param language 语言代码（可选，例如：zh, en）
     * @return 总结消息和更新后的会话信息
     */
    @PostMapping("/sessions/{sessionId}/resolve")
    public ResolveSessionResponse resolveSession(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        UUID agentId = requireAgentAuthAndGetId(authentication);
        
        // 1. 生成并保存总结
        Message summaryMessage = sessionSummaryService.generateAndSaveSummary(sessionId, language);
        
        // 2. 关闭会话
        chatSessionService.closeSession(sessionId);
        
        // 3. 返回结果
        ChatSessionDto sessionDto = chatSessionService.getSessionDto(sessionId, agentId);
        
        return new ResolveSessionResponse(
                sessionDto,
                entityMapper.toMessageDto(summaryMessage)
        );
    }

    /**
     * Resolve 会话响应
     */
    public record ResolveSessionResponse(
            ChatSessionDto session,
            com.example.aikef.dto.MessageDto summaryMessage
    ) {}

    /**
     * 验证客服认证
     */
    private void requireAgentAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AgentPrincipal)) {
            throw new IllegalStateException("需要客服认证");
        }
    }

    /**
     * 验证客服认证并返回客服ID
     */
    private UUID requireAgentAuthAndGetId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AgentPrincipal)) {
            throw new IllegalStateException("需要客服认证");
        }
        return ((AgentPrincipal) authentication.getPrincipal()).getId();
    }
}
