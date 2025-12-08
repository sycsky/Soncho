package com.example.aikef.service;

import com.example.aikef.dto.AttachmentDto;
import com.example.aikef.dto.ChatMessageDto;
import com.example.aikef.model.Message;
import com.example.aikef.model.MessageDelivery;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.MessageDeliveryRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 离线消息服务
 * 只处理客服离线期间的消息推送，客户通过历史消息接口获取消息
 */
@Service
public class OfflineMessageService {

    private static final Logger log = LoggerFactory.getLogger(OfflineMessageService.class);

    private final MessageDeliveryRepository messageDeliveryRepository;

    public OfflineMessageService(MessageDeliveryRepository messageDeliveryRepository) {
        this.messageDeliveryRepository = messageDeliveryRepository;
    }

    /**
     * 获取客服的未发送消息
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getUnsentMessagesForAgent(UUID agentId) {
        log.info("📬 获取客服未发送消息: agentId={}", agentId);
        
        List<MessageDelivery> unsentDeliveries = messageDeliveryRepository.findUnsentForAgent(agentId);
        
        log.info("📬 客服 {} 有 {} 条未发送消息", agentId, unsentDeliveries.size());
        
        // 转换为 DTO（客服视角，显示 agentMetadata）
        return unsentDeliveries.stream()
                .map(delivery -> {
                    Message msg = delivery.getMessage();
                    // 初始化懒加载字段
                    Hibernate.initialize(msg.getAttachments());
                    Hibernate.initialize(msg.getMentionAgentIds());
                    return toMessageDto(msg, true, agentId);
                })
                .collect(Collectors.toList());
    }

    /**
     * 标记客服的消息为已发送
     */
    @Transactional
    public void markAsSentForAgent(UUID agentId) {
        List<MessageDelivery> unsentDeliveries = messageDeliveryRepository.findUnsentForAgent(agentId);
        
        if (!unsentDeliveries.isEmpty()) {
            // 批量更新
            unsentDeliveries.forEach(delivery -> {
                delivery.setSent(true);
                delivery.setSentAt(Instant.now());
            });
            messageDeliveryRepository.saveAll(unsentDeliveries);
            
            log.info("✅ 标记 {} 条消息为已发送 (客服): agentId={}", unsentDeliveries.size(), agentId);
        }
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
        List<String> mentionAgentIds = message.getMentionAgentIds() != null
                ? List.copyOf(message.getMentionAgentIds())
                : List.of();
        
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
                mentionAgentIds,
                attachments,
                agentMetadata,
                message.getCreatedAt()
        );
    }
}
