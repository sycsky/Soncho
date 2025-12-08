package com.example.aikef.service;

import com.example.aikef.model.Agent;
import com.example.aikef.model.AgentMention;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.repository.AgentMentionRepository;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.ChatSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Agent被@记录服务
 */
@Service
@Transactional
public class AgentMentionService {

    private static final Logger log = LoggerFactory.getLogger(AgentMentionService.class);

    private final AgentMentionRepository agentMentionRepository;
    private final AgentRepository agentRepository;
    private final ChatSessionRepository chatSessionRepository;

    public AgentMentionService(AgentMentionRepository agentMentionRepository,
                               AgentRepository agentRepository,
                               ChatSessionRepository chatSessionRepository) {
        this.agentMentionRepository = agentMentionRepository;
        this.agentRepository = agentRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    /**
     * 创建被@记录
     *
     * @param agentId   被@的客服ID
     * @param sessionId 会话ID
     * @param message   关联的消息（可选）
     */
    public AgentMention createMention(UUID agentId, UUID sessionId, Message message) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("客服不存在: " + agentId));

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在: " + sessionId));

        AgentMention mention = new AgentMention();
        mention.setAgent(agent);
        mention.setSession(session);
        mention.setMessage(message);
        mention.setRead(false);

        AgentMention saved = agentMentionRepository.save(mention);
        log.info("创建@记录: agentId={}, sessionId={}, messageId={}",
                agentId, sessionId, message != null ? message.getId() : null);

        return saved;
    }

    /**
     * 批量创建被@记录
     *
     * @param agentIds  被@的客服ID列表
     * @param sessionId 会话ID
     * @param message   关联的消息（可选）
     */
    public void createMentions(List<UUID> agentIds, UUID sessionId, Message message) {
        if (agentIds == null || agentIds.isEmpty()) {
            return;
        }

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在: " + sessionId));

        for (UUID agentId : agentIds) {
            try {
                Agent agent = agentRepository.findById(agentId).orElse(null);
                if (agent != null) {
                    AgentMention mention = new AgentMention();
                    mention.setAgent(agent);
                    mention.setSession(session);
                    mention.setMessage(message);
                    mention.setRead(false);
                    agentMentionRepository.save(mention);
                    log.info("创建@记录: agentId={}, sessionId={}", agentId, sessionId);
                }
            } catch (Exception e) {
                log.warn("创建@记录失败: agentId={}, sessionId={}, error={}", agentId, sessionId, e.getMessage());
            }
        }
    }

    /**
     * 获取客服未读的@记录
     */
    @Transactional(readOnly = true)
    public List<AgentMention> getUnreadMentions(UUID agentId) {
        return agentMentionRepository.findByAgent_IdAndReadFalseOrderByCreatedAtDesc(agentId);
    }

    /**
     * 获取客服所有的@记录
     */
    @Transactional(readOnly = true)
    public List<AgentMention> getAllMentions(UUID agentId) {
        return agentMentionRepository.findByAgent_IdOrderByCreatedAtDesc(agentId);
    }

    /**
     * 统计客服未读的@数量
     */
    @Transactional(readOnly = true)
    public long getUnreadMentionCount(UUID agentId) {
        return agentMentionRepository.countByAgent_IdAndReadFalse(agentId);
    }

    /**
     * 标记客服所有@为已读
     */
    public int markAllAsRead(UUID agentId) {
        int count = agentMentionRepository.markAllAsReadByAgentId(agentId);
        log.info("标记所有@为已读: agentId={}, count={}", agentId, count);
        return count;
    }

    /**
     * 标记指定会话中的@为已读
     */
    public int markAsReadBySession(UUID agentId, UUID sessionId) {
        int count = agentMentionRepository.markAsReadByAgentIdAndSessionId(agentId, sessionId);
        log.info("标记会话@为已读: agentId={}, sessionId={}, count={}", agentId, sessionId, count);
        return count;
    }
}

