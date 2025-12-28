package com.example.aikef.service;

import com.example.aikef.dto.MessageDto;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Agent;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.AttachmentType;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final OfficialChannelMessageService officialChannelMessageService;
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
     * @return 发送的消息（如果是结构化数据，返回最后一条消息）
     */
    @Transactional
    public Message sendMessage(UUID sessionId, String text, SenderType senderType, 
                               UUID agentId, Map<String, Object> metadata, boolean isInternal) {
        // 检查是否是结构化数据（struct# 开头）
        if (text != null && text.startsWith("struct#")) {
            return sendStructuredMessage(sessionId, text, senderType, agentId, metadata, isInternal);
        }

        // 普通消息处理
        return sendSingleMessage(sessionId, text, senderType, agentId, metadata, isInternal, null);
    }

    /**
     * 发送结构化消息（struct# 开头）
     * 解析JSON对象（包含struct数组和overview），将前3条消息的content拼成一条消息，图片作为附件
     * 注意：overview 不会被拼接到消息内容中
     */
    @Transactional
    private Message sendStructuredMessage(UUID sessionId, String text, SenderType senderType,
                                         UUID agentId, Map<String, Object> metadata, boolean isInternal) {
        try {
            // 提取JSON部分（去掉 struct# 前缀）
            String jsonStr = text.substring(7); // "struct#".length() = 7
            
            // 解析JSON对象（包含struct数组和overview）
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            JsonNode structNode = jsonNode.get("struct");
            String overview = jsonNode.has("overview") ? jsonNode.get("overview").asText() : "";

            if (structNode == null || !structNode.isArray() || structNode.size() == 0) {
                log.warn("结构化数据为空，发送原文本");
                return sendSingleMessage(sessionId, text, senderType, agentId, metadata, isInternal, null);
            }

            // 转换为Map列表
            List<Map<String, String>> items = new ArrayList<>();
            for (JsonNode itemNode : structNode) {
                Map<String, String> item = new HashMap<>();
                item.put("img", itemNode.has("img") ? itemNode.get("img").asText() : "");
                item.put("content", itemNode.has("content") ? itemNode.get("content").asText() : "");
                items.add(item);
            }

            if (items.isEmpty()) {
                log.warn("结构化数据为空，发送原文本");
                return sendSingleMessage(sessionId, text, senderType, agentId, metadata, isInternal, null);
            }

            // 限制最多取前3条
            int maxItems = Math.min(items.size(), 3);
            if (items.size() > 3) {
                log.info("结构化数据有 {} 条，限制为最多取前 {} 条", items.size(), maxItems);
            } else {
                log.info("检测到结构化数据，将取前 {} 条消息", items.size());
            }

            // 构建合并的消息内容：前3条消息的content（不包含overview）
            StringBuilder messageContent = new StringBuilder();

            // 添加前3条消息的content，每条消息之间用多个换行分隔
            for (int i = 0; i < maxItems; i++) {
                Map<String, String> item = items.get(i);
                String content = item.getOrDefault("content", "");
                if (content != null && !content.trim().isEmpty()) {
                    // 如果不是第一条，在前面添加多个换行
                    if (messageContent.length() > 0) {
                        messageContent.append("\n\n\n");
                    }
                    messageContent.append(content.trim());
                }
            }

            // 收集前3条消息的图片作为附件
            List<Attachment> attachments = new ArrayList<>();
            for (int i = 0; i < maxItems; i++) {
                Map<String, String> item = items.get(i);
                String img = item.getOrDefault("img", "");
                if (img != null && !img.trim().isEmpty()) {
                    Attachment attachment = new Attachment();
                    attachment.setType(AttachmentType.IMAGE);
                    attachment.setUrl(img.trim());
                    attachment.setName("image_" + (i + 1));
                    attachments.add(attachment);
                }
            }

            // 发送合并后的单条消息（带所有图片附件）
            String finalContent = messageContent.toString().trim();
            if (finalContent.isEmpty()) {
                log.warn("合并后的消息内容为空，发送原文本");
                return sendSingleMessage(sessionId, text, senderType, agentId, metadata, isInternal, null);
            }

            List<Attachment> finalAttachments = attachments.isEmpty() ? null : attachments;
            log.info("发送合并的结构化消息: contentLength={}, attachmentCount={}", 
                    finalContent.length(), finalAttachments != null ? finalAttachments.size() : 0);
            
            return sendSingleMessage(sessionId, finalContent, senderType, agentId, metadata, isInternal, finalAttachments);

        } catch (Exception e) {
            log.error("解析结构化数据失败，发送原文本", e);
            return sendSingleMessage(sessionId, text, senderType, agentId, metadata, isInternal, null);
        }
    }

    /**
     * 发送单条消息（支持附件）
     */
    @Transactional
    private Message sendSingleMessage(UUID sessionId, String text, SenderType senderType,
                                     UUID agentId, Map<String, Object> metadata, boolean isInternal,
                                     List<Attachment> attachments) {
        // 获取会话
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        // 创建消息
        Message message = new Message();
        message.setSession(session);
        message.setText(text);
        message.setSenderType(senderType);
        message.setInternal(isInternal);

        // 设置附件
        if (attachments != null && !attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                attachment.setMessage(message);
                message.getAttachments().add(attachment);
            }
        }

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
            // 先尝试官方渠道（通过SDK，支持附件）
            boolean sentToOfficial = officialChannelMessageService.sendMessageToOfficialChannel(
                    sessionId, text, senderType, attachments);
            if (!sentToOfficial) {
                // 如果不是官方渠道，使用原有的外部平台方式（支持附件）
                externalPlatformService.forwardMessageToExternalPlatform(
                        sessionId, text, senderType, attachments);
            }
        }

        log.info("消息已发送: sessionId={}, type={}, text={}, attachments={}", 
                sessionId, senderType, 
                text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text,
                attachments != null ? attachments.size() : 0);

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

