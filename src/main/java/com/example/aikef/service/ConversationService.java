package com.example.aikef.service;

import com.example.aikef.channel.ChannelRouter;
import com.example.aikef.model.Channel;
import com.example.aikef.dto.ChannelMessage;
import com.example.aikef.dto.MessageDto;
import com.example.aikef.dto.request.AttachmentPayload;
import com.example.aikef.dto.request.SendMessageRequest;
import com.example.aikef.dto.request.UpdateSessionStatusRequest;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Agent;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.MessageDelivery;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SessionAction;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageDeliveryRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.websocket.WebSocketSessionManager;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.hibernate.Hibernate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConversationService {

    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final AgentService agentService;
    private final EntityMapper entityMapper;
    private final ChannelRouter channelRouter;
    private final WebSocketSessionManager sessionManager;
    private final TranslationService translationService;

    public ConversationService(ChatSessionRepository chatSessionRepository,
                               MessageRepository messageRepository,
                               MessageDeliveryRepository messageDeliveryRepository,
                               AgentService agentService,
                               EntityMapper entityMapper,
                               ChannelRouter channelRouter,
                               @Lazy WebSocketSessionManager sessionManager,
                               TranslationService translationService) {
        this.chatSessionRepository = chatSessionRepository;
        this.messageRepository = messageRepository;
        this.messageDeliveryRepository = messageDeliveryRepository;
        this.agentService = agentService;
        this.entityMapper = entityMapper;
        this.channelRouter = channelRouter;
        this.sessionManager = sessionManager;
        this.translationService = translationService;
    }

    public MessageDto sendMessage(SendMessageRequest request, UUID agentId) {
        ChatSession session = loadSession(request.sessionId());
        Agent author = agentId != null ? agentService.findById(agentId) : null;
        boolean isUserMessage = author == null;
        
        Message message = new Message();
        message.setSession(session);
        message.setSenderType(isUserMessage ? SenderType.USER : SenderType.AGENT);
        message.setAgent(author);
        message.setText(request.text());
        message.setInternal(request.isInternal());
        message.setMentionAgentIds(request.mentions() == null ? List.of() : List.copyOf(request.mentions()));
        if (request.attachments() != null) {
            List<Attachment> attachments = request.attachments().stream()
                    .map(payload -> toAttachment(payload, message))
                    .toList();
            message.getAttachments().addAll(attachments);
        }
        
        // 处理翻译
        if (translationService.isEnabled() && request.text() != null && !request.text().isBlank()) {
            String sourceLanguage = null;
            
            if (isUserMessage) {
                // 用户消息：使用会话的客户语言，或自动检测
                sourceLanguage = session.getCustomerLanguage();
                if (sourceLanguage == null || sourceLanguage.isBlank()) {
                    // 第一条用户消息，检测语言并设置到会话
                    sourceLanguage = translationService.detectLanguage(request.text());
                    if (sourceLanguage != null) {
                        session.setCustomerLanguage(sourceLanguage);
                    }
                }
            } else {
                // 客服/AI消息：使用系统默认语言
                sourceLanguage = translationService.getDefaultSystemLanguage();
            }
            
            Map<String, Object> translationData = translationService.translateMessage(
                    request.text(), 
                    sourceLanguage
            );
            message.setTranslationData(translationData);
        }
        
        Message persisted = messageRepository.save(message);
        session.setLastActiveAt(Instant.now());
        
        // 获取在线客服列表
        Set<UUID> onlineAgentIds = sessionManager.getOnlineAgentsInSession(session);
        
        // 创建消息发送记录（在线客服自动标记已读）
        createMessageDeliveries(persisted, session, agentId, onlineAgentIds);

        if (!message.isInternal() && session.getCustomer() != null) {
            Channel Channel = session.getCustomer().getPrimaryChannel();
            ChannelMessage outbound = ChannelMessage.outbound(
                    Channel,
                    session.getId().toString(),
                    author != null ? author.getId().toString() : "system",
                    session.getCustomer().getId().toString(),
                    message.getText(),
                    Map.of("sessionId", session.getId().toString()));
            channelRouter.route(outbound);
        }
        return entityMapper.toMessageDto(persisted);
    }
    
    /**
     * 创建消息发送记录
     * 只为客服创建发送记录，客户通过历史消息接口获取消息
     * 
     * @param message 消息
     * @param session 会话
     * @param senderId 发送者ID（客服或客户）
     * @param onlineAgentIds 当前在线的客服ID集合
     */
    private void createMessageDeliveries(Message message, ChatSession session, UUID senderId, Set<UUID> onlineAgentIds) {
        List<MessageDelivery> deliveries = new ArrayList<>();
        Set<UUID> recipientAgentIds = new HashSet<>();
        
        // 1. 确定所有应该收到消息的客服
        if (session.getPrimaryAgent() != null) {
            recipientAgentIds.add(session.getPrimaryAgent().getId());
        }
        if (session.getSupportAgentIds() != null) {
            recipientAgentIds.addAll(session.getSupportAgentIds());
        }
        
        // 2. 为每个客服创建发送记录
        for (UUID agentId : recipientAgentIds) {
            MessageDelivery delivery = new MessageDelivery();
            delivery.setMessage(message);
            delivery.setAgentId(agentId);
            
            // 标记为已读的情况：
            // 1. 发送者本人（客服发送消息）
            // 2. 在线的客服（已通过WebSocket收到消息）
            if (agentId.equals(senderId) || onlineAgentIds.contains(agentId)) {
                delivery.setSent(true);
                delivery.setSentAt(Instant.now());
            }
            
            deliveries.add(delivery);
        }
        
        // 3. 批量保存（注意：不为客户创建发送记录，客户通过历史消息接口获取）
        if (!deliveries.isEmpty()) {
            messageDeliveryRepository.saveAll(deliveries);
        }
    }

    public ChatSession updateSessionStatus(UpdateSessionStatusRequest request) {
        ChatSession session = loadSession(request.sessionId());
        SessionStatus action = request.action();
        switch (action) {
            case RESOLVED -> session.setStatus(SessionStatus.RESOLVED);
            case AI_HANDLING -> session.setStatus(SessionStatus.AI_HANDLING);
            case HUMAN_HANDLING -> session.setStatus(SessionStatus.HUMAN_HANDLING);
        }
        return session;
    }

    

    private ChatSession loadSession(String sessionId) {
        UUID id = UUID.fromString(sessionId);
        return chatSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
    }

    private Attachment toAttachment(AttachmentPayload payload, Message message) {
        Attachment attachment = new Attachment();
        attachment.setMessage(message);
        attachment.setType(payload.type());
        attachment.setUrl(payload.url());
        attachment.setName(payload.name());
        attachment.setSizeKb(payload.sizeKb());
        return attachment;
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private UUID extractAgentId(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("newOwnerAgentId")) {
            throw new IllegalArgumentException("转派操作需要提供 newOwnerAgentId");
        }
        return UUID.fromString(String.valueOf(payload.get("newOwnerAgentId")));
    }
    
    /**
     * 获取聊天会话
     */
    public ChatSession getChatSession(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .map(session -> {
                    Hibernate.initialize(session.getSupportAgentIds());
                    return session;
                })
                .orElse(null);
    }
}
