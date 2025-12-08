package com.example.aikef.service;

import com.example.aikef.dto.MessageDto;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Agent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 会话消息统一入口服务
 * 
 * 用于 AI 工作流、系统消息等场景发送消息到会话
 * 支持模拟发送：
 * - AI 消息
 * - 客户消息
 * - 客服消息
 * - 系统消息
 * 
 * 发送的消息会：
 * 1. 保存到数据库
 * 2. 通过 WebSocket 推送给会话中的所有参与者
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionMessageGateway {

    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final AgentService agentService;
    private final WebSocketSessionManager sessionManager;
    private final EntityMapper entityMapper;
    private final ObjectMapper objectMapper;
    private final ExternalPlatformService externalPlatformService;
    private final TranslationService translationService;

    /**
     * 发送 AI 消息
     * 
     * @param sessionId 会话ID
     * @param text      消息文本
     * @return 发送的消息
     */
    @Transactional
    public Message sendAiMessage(UUID sessionId, String text) {
        return sendMessage(sessionId, text, SenderType.AI, null, null, false);
    }

    /**
     * 发送 AI 消息（带元数据）
     * 
     * @param sessionId     会话ID
     * @param text          消息文本
     * @param metadata      AI 元数据（如工作流ID、节点ID等）
     * @return 发送的消息
     */
    @Transactional
    public Message sendAiMessage(UUID sessionId, String text, Map<String, Object> metadata) {
        return sendMessage(sessionId, text, SenderType.AI, null, metadata, false);
    }

    /**
     * 模拟客户发送消息
     * 
     * @param sessionId 会话ID
     * @param text      消息文本
     * @return 发送的消息
     */
    @Transactional
    public Message sendAsCustomer(UUID sessionId, String text) {
        return sendMessage(sessionId, text, SenderType.USER, null, null, false);
    }

    /**
     * 模拟客服发送消息
     * 
     * @param sessionId 会话ID
     * @param text      消息文本
     * @param agentId   客服ID（可选，为空时不关联具体客服）
     * @return 发送的消息
     */
    @Transactional
    public Message sendAsAgent(UUID sessionId, String text, UUID agentId) {
        return sendMessage(sessionId, text, SenderType.AGENT, agentId, null, false);
    }

    /**
     * 发送系统消息
     * 
     * @param sessionId 会话ID
     * @param text      消息文本
     * @return 发送的消息
     */
    @Transactional
    public Message sendSystemMessage(UUID sessionId, String text) {
        return sendMessage(sessionId, text, SenderType.SYSTEM, null, null, false);
    }

    /**
     * 发送内部消息（仅客服可见）
     * 
     * @param sessionId  会话ID
     * @param text       消息文本
     * @param senderType 发送者类型
     * @param agentId    客服ID（可选）
     * @return 发送的消息
     */
    @Transactional
    public Message sendInternalMessage(UUID sessionId, String text, SenderType senderType, UUID agentId) {
        return sendMessage(sessionId, text, senderType, agentId, null, true);
    }

    /**
     * 统一发送消息方法
     * 
     * @param sessionId   会话ID
     * @param text        消息文本
     * @param senderType  发送者类型
     * @param agentId     客服ID（可选）
     * @param metadata    元数据（可选）
     * @param isInternal  是否内部消息
     * @return 发送的消息
     */
    @Transactional
    public Message sendMessage(UUID sessionId, String text, SenderType senderType, 
                               UUID agentId, Map<String, Object> metadata, boolean isInternal) {
        // 获取会话
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        // 创建消息
        Message message = new Message();
        message.setSession(session);
        message.setText(text);
        message.setSenderType(senderType);
        message.setInternal(isInternal);

        // 设置客服（如果有）
        if (agentId != null && (senderType == SenderType.AGENT || senderType == SenderType.AI)) {
            Agent agent = agentService.findById(agentId);
            message.setAgent(agent);
        }

        // 设置元数据
        if (metadata != null && !metadata.isEmpty()) {
            message.setAgentMetadata(new HashMap<>(metadata));
        }

        // 处理翻译
        if (translationService.isEnabled() && text != null && !text.isBlank()) {
            String sourceLanguage;
            if (senderType == SenderType.USER) {
                // 用户消息：使用会话的客户语言，或自动检测
                sourceLanguage = session.getCustomerLanguage();
                if (sourceLanguage == null || sourceLanguage.isBlank()) {
                    sourceLanguage = translationService.detectLanguage(text);
                    if (sourceLanguage != null) {
                        session.setCustomerLanguage(sourceLanguage);
                    }
                }
            } else {
                // AI/客服/系统消息：使用系统默认语言
                sourceLanguage = translationService.getDefaultSystemLanguage();
            }
            
            Map<String, Object> translationData = translationService.translateMessage(text, sourceLanguage);
            message.setTranslationData(translationData);
        }

        // 保存消息
        Message saved = messageRepository.save(message);

        // 更新会话最后活跃时间
        session.setLastActiveAt(Instant.now());
        chatSessionRepository.save(session);

        // 广播消息到 WebSocket
        broadcastMessage(session, saved);

        // 转发到第三方平台（AI 和客服消息需要转发，客户消息不需要）
        if (senderType == SenderType.AI || senderType == SenderType.AGENT || senderType == SenderType.SYSTEM) {
            externalPlatformService.forwardMessageToExternalPlatform(sessionId, text, senderType);
        }

        log.info("消息已发送: sessionId={}, type={}, text={}", 
                sessionId, senderType, text.length() > 50 ? text.substring(0, 50) + "..." : text);

        return saved;
    }

    /**
     * 广播消息到 WebSocket
     */
    private void broadcastMessage(ChatSession session, Message message) {
        try {
            // 转换为 DTO
            MessageDto messageDto = entityMapper.toMessageDto(message);

            // 构建广播事件
            ServerEvent broadcastEvent = new ServerEvent("newMessage", Map.of(
                    "sessionId", session.getId().toString(),
                    "message", messageDto));

            String broadcastJson = objectMapper.writeValueAsString(broadcastEvent);

            // 根据消息类型决定发送者ID（AI和系统消息没有真实发送者）
            UUID senderId = null;
            if (message.getSenderType() == SenderType.USER && session.getCustomer() != null) {
                senderId = session.getCustomer().getId();
            } else if (message.getSenderType() == SenderType.AGENT && message.getAgent() != null) {
                senderId = message.getAgent().getId();
            }

            // 广播给会话的所有参与者
            sessionManager.broadcastToSession(
                    session.getId(),
                    session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null,
                    session.getSupportAgentIds() != null ? session.getSupportAgentIds().stream().toList() : null,
                    session.getCustomer() != null ? session.getCustomer().getId() : null,
                    senderId,
                    broadcastJson
            );

            log.debug("消息已广播: sessionId={}, messageId={}", session.getId(), message.getId());

        } catch (Exception e) {
            log.error("广播消息失败: sessionId={}, messageId={}", session.getId(), message.getId(), e);
        }
    }

    /**
     * 批量发送消息（不立即广播，用于批量场景）
     */
    @Transactional
    public Message sendMessageWithoutBroadcast(UUID sessionId, String text, SenderType senderType,
                                               UUID agentId, Map<String, Object> metadata, boolean isInternal) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        Message message = new Message();
        message.setSession(session);
        message.setText(text);
        message.setSenderType(senderType);
        message.setInternal(isInternal);

        if (agentId != null && (senderType == SenderType.AGENT || senderType == SenderType.AI)) {
            Agent agent = agentService.findById(agentId);
            message.setAgent(agent);
        }

        if (metadata != null && !metadata.isEmpty()) {
            message.setAgentMetadata(new HashMap<>(metadata));
        }

        return messageRepository.save(message);
    }

    /**
     * 手动触发消息广播
     */
    public void broadcast(Message message) {
        if (message != null && message.getSession() != null) {
            broadcastMessage(message.getSession(), message);
        }
    }

    /**
     * 发送 AI 工作流消息
     * 
     * @param sessionId  会话ID
     * @param text       消息文本
     * @param workflowId 工作流ID
     * @param nodeId     节点ID
     * @return 发送的消息
     */
    @Transactional
    public Message sendWorkflowMessage(UUID sessionId, String text, UUID workflowId, String nodeId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "workflow");
        metadata.put("workflowId", workflowId.toString());
        if (nodeId != null) {
            metadata.put("nodeId", nodeId);
        }
        return sendAiMessage(sessionId, text, metadata);
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(UUID sessionId) {
        return chatSessionRepository.existsById(sessionId);
    }

    /**
     * 获取会话分类ID
     */
    public UUID getSessionCategoryId(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .map(session -> session.getCategory() != null ? session.getCategory().getId() : null)
                .orElse(null);
    }
}

