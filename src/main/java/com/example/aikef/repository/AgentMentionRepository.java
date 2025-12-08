package com.example.aikef.repository;

import com.example.aikef.model.AgentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentMentionRepository extends JpaRepository<AgentMention, UUID> {

    /**
     * 根据客服ID查询所有被@记录
     */
    List<AgentMention> findByAgent_IdOrderByCreatedAtDesc(UUID agentId);

    /**
     * 根据客服ID查询未读的被@记录
     */
    List<AgentMention> findByAgent_IdAndReadFalseOrderByCreatedAtDesc(UUID agentId);

    /**
     * 根据会话ID查询所有被@记录
     */
    List<AgentMention> findBySession_IdOrderByCreatedAtDesc(UUID sessionId);

    /**
     * 根据客服ID和会话ID查询被@记录
     */
    List<AgentMention> findByAgent_IdAndSession_IdOrderByCreatedAtDesc(UUID agentId, UUID sessionId);

    /**
     * 统计客服未读的@数量
     */
    long countByAgent_IdAndReadFalse(UUID agentId);

    /**
     * 标记客服的所有@为已读
     */
    @Modifying
    @Query("UPDATE AgentMention m SET m.read = true WHERE m.agent.id = :agentId AND m.read = false")
    int markAllAsReadByAgentId(@Param("agentId") UUID agentId);

    /**
     * 标记指定会话中客服的@为已读
     */
    @Modifying
    @Query("UPDATE AgentMention m SET m.read = true WHERE m.agent.id = :agentId AND m.session.id = :sessionId AND m.read = false")
    int markAsReadByAgentIdAndSessionId(@Param("agentId") UUID agentId, @Param("sessionId") UUID sessionId);

    /**
     * 根据消息ID查询被@记录
     */
    List<AgentMention> findByMessage_Id(UUID messageId);

    /**
     * 统计指定会话中客服在某时间之后的@数量
     * 用于计算支持客服的未读@数
     */
    @Query("SELECT COUNT(m) FROM AgentMention m WHERE m.agent.id = :agentId AND m.session.id = :sessionId AND m.createdAt > :lastReadTime")
    long countByAgentIdAndSessionIdAndCreatedAtAfter(
            @Param("agentId") UUID agentId,
            @Param("sessionId") UUID sessionId,
            @Param("lastReadTime") java.time.Instant lastReadTime);

    /**
     * 统计指定会话中客服的所有@数量（没有已读记录时使用）
     */
    long countByAgent_IdAndSession_Id(UUID agentId, UUID sessionId);
}

