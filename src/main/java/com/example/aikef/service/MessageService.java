package com.example.aikef.service;

import com.example.aikef.dto.AttachmentDto;
import com.example.aikef.dto.ChatMessageDto;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务
 */
@Service
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatSessionService chatSessionService;
    private final AgentService agentService;
    private final ReadRecordService readRecordService;

    public MessageService(MessageRepository messageRepository,
                         ChatSessionService chatSessionService,
                         AgentService agentService,
                         @Lazy ReadRecordService readRecordService) {
        this.messageRepository = messageRepository;
        this.chatSessionService = chatSessionService;
        this.agentService = agentService;
        this.readRecordService = readRecordService;
    }

    /**
     * 获取群组历史消息
     * 根据调用者身份返回不同的内容：
     * - 客服：包含所有消息和 agentMetadata
     * - 客户：只包含非内部消息（isInternal = false），不包含 agentMetadata
     */
    public Page<ChatMessageDto> getSessionMessages(UUID sessionId, 
                                                   AgentPrincipal agentPrincipal,
                                                   CustomerPrincipal customerPrincipal,
                                                   Pageable pageable) {
        // 验证权限
        UUID agentId = agentPrincipal != null ? agentPrincipal.getId() : null;
        UUID customerId = customerPrincipal != null ? customerPrincipal.getId() : null;
        
        if (!chatSessionService.isSessionMember(sessionId, agentId, customerId)) {
            throw new SecurityException("无权访问此会话的消息");
        }
        
        boolean isAgent = agentPrincipal != null;
        boolean isCustomer = customerPrincipal != null;
        UUID currentUserId = isAgent ? agentId : customerId;
        
        ChatSession session = chatSessionService.getSession(sessionId);
        
        // 排除的消息类型
        List<SenderType> excludedTypes = List.of(SenderType.TOOL, SenderType.AI_TOOL_REQUEST);

        // 客户只能看到非内部消息
        // 按创建时间倒序查询，最新的消息在前
        Page<Message> messages;
        if (isCustomer) {
            messages = messageRepository.findBySession_IdAndInternalFalseAndSenderTypeNotInOrderByCreatedAtDesc(
                    sessionId, excludedTypes, pageable);
        } else {
            messages = messageRepository.findBySession_IdAndSenderTypeNotInOrderByCreatedAtDesc(
                    sessionId, excludedTypes, pageable);
        }
        
        return messages.map(message -> toMessageDto(message, isAgent, currentUserId));
    }

    /**
     * 转换消息为 DTO
     */
    private ChatMessageDto toMessageDto(Message message, boolean isAgent, UUID currentUserId) {
        // 判断是否是本人发送的
        boolean isMine = false;
        if (message.getSenderType() == SenderType.AGENT && message.getAgent() != null) {
            isMine = message.getAgent().getId().equals(currentUserId);
        } else if (message.getSenderType() == SenderType.USER 
                   && message.getSession().getCustomer() != null) {
            isMine = message.getSession().getCustomer().getId().equals(currentUserId);
        }
        
        // 客服可见的元数据（客户看不到）
        Map<String, Object> agentMetadata = isAgent ? message.getAgentMetadata() : null;
        
        List<AttachmentDto> attachments = message.getAttachments().stream()
                .map(att -> new AttachmentDto(
                        att.getId(),
                        att.getType(),
                        att.getUrl(),
                        att.getName(),
                        att.getSizeKb()
                ))
                .collect(Collectors.toList());
        
        String agentName = message.getAgent() != null ? message.getAgent().getName() : null;
        
        return new ChatMessageDto(
                message.getId(),
                message.getSession().getId(),
                message.getSenderType(),
                message.getAgent() != null ? message.getAgent().getId() : null,
                agentName,
                message.getText(),
                message.isInternal(),
                isMine,
                message.getTranslationData(),
                new ArrayList<>(message.getMentionAgentIds()),
                attachments,
                agentMetadata,
                message.getCreatedAt()
        );
    }

    /**
     * 发送消息
     */
    @Transactional
    public Message sendMessage(UUID sessionId,
                               String text,
                               SenderType senderType,
                               UUID agentId,
                               Map<String, Object> agentMetadata) {
        ChatSession session = chatSessionService.getSession(sessionId);
        
        Message message = new Message();
        message.setSession(session);
        message.setText(text);
        message.setSenderType(senderType);
        message.setInternal(false);
        
        if (agentId != null && senderType == SenderType.AGENT) {
            message.setAgent(agentService.findById(agentId));
        }
        
        if (agentMetadata != null && !agentMetadata.isEmpty()) {
            message.setAgentMetadata(new HashMap<>(agentMetadata));
        }
        
        Message saved = messageRepository.save(message);
        
        // 更新会话最后活跃时间
        chatSessionService.updateLastActiveTime(sessionId);
        
        // 客服发送消息时，更新已读记录时间（因为发送消息意味着已经看过之前的消息）
        if (agentId != null && senderType == SenderType.AGENT) {
            readRecordService.updateReadTime(sessionId, agentId);
        }
        
        return saved;
    }
}
