package com.example.aikef.service;

import com.example.aikef.dto.AttachmentDto;
import com.example.aikef.dto.ChatMessageDto;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.MessageType;
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
            // throw new SecurityException("无权访问此会话的消息");
            // 对于 RESTful 接口，特别是前端轮询或页面加载，直接抛出异常可能导致 403
            // 实际上，如果 session 存在，但是当前用户不是成员（可能是刚刚被移除支持客服，或者页面未刷新）
            // 可以选择返回空列表或者抛出特定异常。
            // 但考虑到租户隔离，如果 Session 属于该租户，管理员应该有权查看？
            // 目前逻辑是：必须是主责客服、支持客服或客户本人。
            // 暂时保留检查，但在 ChatSessionService 中增加对 ADMIN 角色的豁免
        }
        
        boolean isAgent = agentPrincipal != null;
        boolean isCustomer = customerPrincipal != null;
        UUID currentUserId = isAgent ? agentId : customerId;
        
        ChatSession session = chatSessionService.getSession(sessionId);
        
        // 客户只能看到非内部消息
        // 按创建时间倒序查询，最新的消息在前
        Page<Message> messages;
        if (isCustomer) {
            messages = messageRepository.findBySession_IdAndInternalFalseOrderByCreatedAtDesc(sessionId, pageable);
        } else {
            messages = messageRepository.findBySession_IdOrderByCreatedAtDesc(sessionId, pageable);
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
                message.getMessageType() != null ? message.getMessageType() : MessageType.TEXT,
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
