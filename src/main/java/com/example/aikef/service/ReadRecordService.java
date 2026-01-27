package com.example.aikef.service;

import com.example.aikef.model.Agent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.ReadRecord;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.AgentMentionRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.repository.ReadRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 已读记录服务
 * 管理客服对会话的已读状态
 */
@Service
@Transactional(readOnly = true)
public class ReadRecordService {

    private final ReadRecordRepository readRecordRepository;
    private final MessageRepository messageRepository;
    private final ChatSessionService chatSessionService;
    private final AgentService agentService;
    private final AgentMentionRepository agentMentionRepository;

    public ReadRecordService(ReadRecordRepository readRecordRepository,
                            MessageRepository messageRepository,
                            @Lazy ChatSessionService chatSessionService,
                            AgentService agentService,
                            AgentMentionRepository agentMentionRepository) {
        this.readRecordRepository = readRecordRepository;
        this.messageRepository = messageRepository;
        this.chatSessionService = chatSessionService;
        this.agentService = agentService;
        this.agentMentionRepository = agentMentionRepository;
    }

    /**
     * 更新已读时间
     * 当客服打开会话时调用
     */
    @Transactional
    public void updateReadTime(UUID sessionId, UUID agentId) {
        ChatSession session = chatSessionService.findById(sessionId);
        Agent agent = agentService.findById(agentId);

        ReadRecord record = readRecordRepository
                .findBySessionIdAndAgentId(sessionId, agentId)
                .orElse(null);

        if (record == null) {
            // 创建新记录
            record = new ReadRecord();
            record.setSession(session);
            record.setAgent(agent);
        }

        record.setLastReadTime(Instant.now());
        readRecordRepository.save(record);
    }

    /**
     * 获取会话的未读消息数
     * 计算逻辑: messages.created_at > read_record.last_read_time
     * 注意：排除 SYSTEM 类型的消息（如会话总结、系统通知）
     */
    public int getUnreadCount(UUID sessionId, UUID agentId) {
        Optional<ReadRecord> recordOpt = readRecordRepository
                .findBySessionIdAndAgentId(sessionId, agentId);

        if (recordOpt.isEmpty()) {
            // 没有已读记录,所有消息都是未读的 (排除 SYSTEM)
            return (int) messageRepository.countBySession_IdAndSenderTypeNot(sessionId, SenderType.SYSTEM);
        }

        Instant lastReadTime = recordOpt.get().getLastReadTime();
        
        // 统计在最后已读时间之后创建的消息数 (排除 SYSTEM)
        return (int) messageRepository.countBySession_IdAndCreatedAtAfterAndSenderTypeNot(
                sessionId, lastReadTime, SenderType.SYSTEM);
    }

    /**
     * 批量获取多个会话的未读消息数
     * 用于优化bootstrap接口性能
     */
    public Map<UUID, Integer> getUnreadCountBatch(List<UUID> sessionIds, UUID agentId) {
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询已读记录
        List<ReadRecord> records = readRecordRepository
                .findByAgentIdAndSessionIdIn(agentId, sessionIds);

        Map<UUID, Instant> lastReadTimeMap = records.stream()
                .collect(Collectors.toMap(
                    r -> r.getSession().getId(),
                    ReadRecord::getLastReadTime
                ));

        // 批量计算未读数
        Map<UUID, Integer> unreadCountMap = new HashMap<>();
        for (UUID sessionId : sessionIds) {
            Instant lastReadTime = lastReadTimeMap.get(sessionId);
            
            int unreadCount;
            if (lastReadTime == null) {
                // 没有已读记录,所有消息都未读
                unreadCount = (int) messageRepository.countBySession_Id(sessionId);
            } else {
                // 统计最后已读时间之后的消息
                unreadCount = (int) messageRepository.countBySession_IdAndCreatedAtAfter(
                    sessionId, lastReadTime);
            }
            
            unreadCountMap.put(sessionId, unreadCount);
        }

        return unreadCountMap;
    }

    /**
     * 获取客服的所有未读会话数量
     */
    public long getTotalUnreadSessions(UUID agentId) {
        List<ReadRecord> records = readRecordRepository.findByAgentId(agentId);
        
        return records.stream()
                .filter(record -> {
                    int unreadCount = getUnreadCount(
                        record.getSession().getId(), 
                        agentId
                    );
                    return unreadCount > 0;
                })
                .count();
    }

    /**
     * 获取支持客服的未读@数量
     * 统计 mention 记录中创建时间大于最后阅读时间的条数
     *
     * @param sessionId 会话ID
     * @param agentId 客服ID
     * @return 未读@数量
     */
    public int getMentionUnreadCount(UUID sessionId, UUID agentId) {
        Optional<ReadRecord> recordOpt = readRecordRepository
                .findBySessionIdAndAgentId(sessionId, agentId);

        if (recordOpt.isEmpty()) {
            // 没有已读记录，返回该会话中该客服所有的@数量
            return (int) agentMentionRepository.countByAgent_IdAndSession_Id(agentId, sessionId);
        }

        Instant lastReadTime = recordOpt.get().getLastReadTime();
        
        // 统计创建时间大于最后阅读时间的@数量
        return (int) agentMentionRepository.countByAgentIdAndSessionIdAndCreatedAtAfter(
                agentId, sessionId, lastReadTime);
    }

    /**
     * 批量获取支持客服的未读@数量
     *
     * @param sessionIds 会话ID列表
     * @param agentId 客服ID
     * @return 会话ID -> 未读@数量的映射
     */
    public Map<UUID, Integer> getMentionUnreadCountBatch(List<UUID> sessionIds, UUID agentId) {
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询已读记录
        List<ReadRecord> records = readRecordRepository
                .findByAgentIdAndSessionIdIn(agentId, sessionIds);

        Map<UUID, Instant> lastReadTimeMap = records.stream()
                .collect(Collectors.toMap(
                    r -> r.getSession().getId(),
                    ReadRecord::getLastReadTime
                ));

        // 计算每个会话的未读@数量
        Map<UUID, Integer> unreadCountMap = new HashMap<>();
        for (UUID sessionId : sessionIds) {
            Instant lastReadTime = lastReadTimeMap.get(sessionId);
            
            int unreadCount;
            if (lastReadTime == null) {
                // 没有已读记录，返回该会话中所有的@数量
                unreadCount = (int) agentMentionRepository.countByAgent_IdAndSession_Id(agentId, sessionId);
            } else {
                // 统计创建时间大于最后阅读时间的@数量
                unreadCount = (int) agentMentionRepository.countByAgentIdAndSessionIdAndCreatedAtAfter(
                        agentId, sessionId, lastReadTime);
            }
            
            unreadCountMap.put(sessionId, unreadCount);
        }

        return unreadCountMap;
    }
}
