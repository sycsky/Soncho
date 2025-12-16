package com.example.aikef.workflow.util;

import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 历史消息加载工具类
 * 用于统一加载工作流节点所需的历史消息记录
 */
@Slf4j
@Component
public class HistoryMessageLoader {

    private final MessageRepository messageRepository;

    public HistoryMessageLoader(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 加载历史消息
     * 
     * @param sessionId 会话ID
     * @param readCount 读取数量（排除 SYSTEM 消息和空消息后的数量）
     * @param messageId 触发工作流的消息ID（可为null，用于时间过滤）
     * @return 历史消息列表（按时间正序，已过滤 SYSTEM 消息和空消息）
     */
    public List<Message> loadHistoryMessages(UUID sessionId, int readCount, UUID messageId) {
        List<Message> historyMessages = new ArrayList<>();
        
        try {
            // 获取触发工作流的消息的创建时间（如果提供了messageId）
            Instant maxCreatedAt;
            Message triggerMessage = null;
            if (messageId != null) {
                triggerMessage = messageRepository.findById(messageId).orElse(null);
                if (triggerMessage != null) {
                    maxCreatedAt = triggerMessage.getCreatedAt();
                    log.debug("根据触发消息时间过滤历史记录: sessionId={}, messageId={}, createdAt={}", 
                            sessionId, messageId, maxCreatedAt);
                } else {
                    maxCreatedAt = null;
                }
            } else {
                maxCreatedAt = null;
            }

            // 为了确保过滤掉 SYSTEM 消息和空消息后仍有足够数量的消息，查询更多消息
            // 查询数量设为 readCount * 2，以确保过滤后仍有足够的消息
            int queryCount = Math.max(readCount * 2, 50); // 至少查询50条，确保有足够的数据
            
            // 根据是否有时间限制选择查询方法
            List<Message> dbMessages;
            if (maxCreatedAt != null) {
                // 为了避免时间精度问题，查询时使用 maxCreatedAt + 1秒 作为上限
                // 然后在内存中过滤，确保只包含创建时间 <= maxCreatedAt 的消息
                // 这样可以确保当前消息（创建时间 = maxCreatedAt）一定能被查到
                Instant queryUpperBound = maxCreatedAt.plusSeconds(1);
                dbMessages = messageRepository.findBySession_IdAndInternalFalseAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                        sessionId, queryUpperBound, PageRequest.of(0, queryCount));
                
                // 在内存中过滤，只保留创建时间 <= maxCreatedAt 的消息
                dbMessages = dbMessages.stream()
                        .filter(msg -> msg.getCreatedAt().isBefore(maxCreatedAt) || 
                                      msg.getCreatedAt().equals(maxCreatedAt))
                        .collect(Collectors.toList());
                
                // 确保触发消息本身被包含（如果它符合条件）
                if (triggerMessage != null && 
                    triggerMessage.getSenderType() != SenderType.SYSTEM &&
                    triggerMessage.getText() != null && !triggerMessage.getText().isEmpty() &&
                    !dbMessages.stream().anyMatch(msg -> msg.getId().equals(messageId))) {
                    // 如果触发消息不在结果中，添加到列表（按时间排序）
                    dbMessages.add(triggerMessage);
                    dbMessages.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                }
            } else {
                // 查询最近的N条消息（按时间倒序查询，最新的在前）
                dbMessages = messageRepository.findBySession_IdAndInternalFalseOrderByCreatedAtDesc(
                        sessionId, PageRequest.of(0, queryCount));
            }
            
            if (dbMessages.isEmpty()) {
                return historyMessages;
            }
            
            // 先过滤掉 SYSTEM 类型的消息和空消息，保留最新的消息
            List<Message> filteredMessages = new ArrayList<>();
            for (Message msg : dbMessages) {
                // 忽略 SYSTEM 类型的消息
                if (msg.getSenderType() == SenderType.SYSTEM) {
                    continue;
                }
                
                String text = msg.getText();
                if (text == null || text.isEmpty()) {
                    continue;
                }
                
                filteredMessages.add(msg);
                
                // 达到请求的数量后停止
                if (filteredMessages.size() >= readCount) {
                    break;
                }
            }
            
            // 反转列表，使其按时间正序排列（最老的在前，最新的在后）
            java.util.Collections.reverse(filteredMessages);
            
            historyMessages = filteredMessages;
            
            log.debug("加载历史消息: sessionId={}, messageId={}, 请求条数={}, 实际条数={}", 
                    sessionId, messageId, readCount, historyMessages.size());
                    
        } catch (Exception e) {
            log.warn("加载历史消息失败: sessionId={}, messageId={}, error={}", 
                    sessionId, messageId, e.getMessage(), e);
        }
        
        return historyMessages;
    }
}

