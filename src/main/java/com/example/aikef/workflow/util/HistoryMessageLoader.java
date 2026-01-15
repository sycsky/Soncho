package com.example.aikef.workflow.util;

import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
                    log.warn("无法找到触发消息: sessionId={}, messageId={}，将查询所有历史消息", 
                            sessionId, messageId);
                    maxCreatedAt = null;
                }
            } else {
                maxCreatedAt = null;
            }

            // 分页查询消息，每次查询一部分，直到满足条件或没有更多消息
            // 初始查询数量适当放大，以减少查询次数
            int pageSize = Math.max(readCount * 2, 50); 
            int pageNum = 0;
            List<Message> candidateMessages = new ArrayList<>();
            boolean stopCollecting = false;
            int effectiveCount = 0; // 记录有效消息数量（不含 TOOL 消息）

            while (!stopCollecting) {
                List<Message> batchMessages;
                PageRequest pageRequest = PageRequest.of(pageNum, pageSize);

                if (maxCreatedAt != null) {
                    // 查询 <= maxCreatedAt 的消息，倒序
                    // 使用 maxCreatedAt + 1秒 作为上限，然后在内存过滤
                    Instant queryUpperBound = maxCreatedAt.plusSeconds(1);
                    batchMessages = messageRepository.findBySession_IdAndInternalFalseAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                            sessionId, queryUpperBound, pageRequest);
                    
                    // 过滤掉晚于 maxCreatedAt 的消息
                    batchMessages = batchMessages.stream()
                            .filter(msg -> !msg.getCreatedAt().isAfter(maxCreatedAt))
                            .collect(Collectors.toList());
                } else {
                    // 查询最近消息，倒序
                    Page<Message> messagePage = messageRepository.findBySession_IdAndInternalFalseOrderByCreatedAtDesc(
                            sessionId, pageRequest);
                    batchMessages = messagePage.getContent();
                }

                if (batchMessages.isEmpty()) {
                    break; // 没有更多消息了
                }

                // 处理当前批次
                for (Message msg : batchMessages) {
                    // 遇到 SYSTEM 消息，停止收集之前的所有消息（包括本条 SYSTEM 及其之后的消息都不需要）
                    // 题目要求：遇到 SYSTEM 就截止，只查 SYSTEM 之前的消息
                    if (msg.getSenderType() == SenderType.SYSTEM) {
                        stopCollecting = true;
                        break;
                    }


                    if (msg.getSenderType()!=SenderType.AI_TOOL_REQUEST && (msg.getText() == null || msg.getText().isEmpty())) {
                        continue;
                    }

                    candidateMessages.add(msg);

                    // 计数逻辑：TOOL 消息不算 readCount
                    if (msg.getSenderType() != SenderType.TOOL) {
                        effectiveCount++;
                    }
                    
                    // 检查是否满足 readCount
                    // 注意：readCount = 0 时，需要一直往上读，直到遇到普通消息？
                    // 题目要求："读取的时候readCount=0时需要往上再读直到碰到普通的消息"
                    // 这句话理解为：如果 readCount > 0，则收集到 effectiveCount == readCount 为止。
                    // 如果 readCount == 0 (或初始状态)，可能是指需要至少找到一条普通消息？
                    // 但通常 readCount 是外部传入的期望获取的历史对话轮数。
                    // 假设 readCount 是指 "非 Tool 类型的消息数量"。
                    
                    if (readCount > 0 && effectiveCount >= readCount) {
                        stopCollecting = true;
                        break;
                    }
                }

                if (stopCollecting) {
                    break;
                }

                pageNum++; // 继续下一页
                
                // 防止无限循环（比如数据库全是 TOOL 消息且没有 SYSTEM 消息）
                if (pageNum > 20) { // 最多查 20 * 50 = 1000 条
                    log.warn("历史消息查询超过深度限制，强制停止");
                    break;
                }
            }

            // 如果触发消息存在且符合条件，且不在结果中，需要添加进去？
            // 之前的逻辑是这样的。现在的逻辑是按时间倒序查的。
            // 如果触发消息在时间范围内，它应该已经被查到了。
            // 如果触发消息是 SYSTEM，那它会触发截止。
            
            // 反转列表，按时间正序排列
            java.util.Collections.reverse(candidateMessages);
            historyMessages = candidateMessages;
            
            log.debug("加载历史消息: sessionId={}, messageId={}, 请求readCount={}, 实际返回={}, 有效条数={}", 
                    sessionId, messageId, readCount, historyMessages.size(), effectiveCount);
                    
        } catch (Exception e) {
            log.warn("加载历史消息失败: sessionId={}, messageId={}, error={}", 
                    sessionId, messageId, e.getMessage(), e);
        }
        
        return historyMessages;
    }
}

