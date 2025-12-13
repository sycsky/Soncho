package com.example.aikef.repository;

import com.example.aikef.model.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Agent 会话 Repository
 */
@Repository
public interface AgentSessionRepository extends JpaRepository<AgentSession, UUID> {

    /**
     * 根据会话ID和工作流ID查找未结束的 AgentSession
     */
    @Query("SELECT a FROM AgentSession a WHERE a.sessionId = :sessionId " +
           "AND a.workflow.id = :workflowId AND a.ended = false")
    Optional<AgentSession> findBySessionIdAndWorkflowIdAndNotEnded(
            @Param("sessionId") UUID sessionId,
            @Param("workflowId") UUID workflowId);

    /**
     * 根据会话ID查找未结束的 AgentSession（任意工作流）
     */
    @Query("SELECT a FROM AgentSession a WHERE a.sessionId = :sessionId AND a.ended = false")
    Optional<AgentSession> findBySessionIdAndNotEnded(@Param("sessionId") UUID sessionId);

    /**
     * 根据会话ID查找未结束的 AgentSession（任意工作流）
     * 返回列表版本（以防有多个）
     */
    @Query("SELECT a FROM AgentSession a WHERE a.sessionId = :sessionId AND a.ended = false")
    java.util.List<AgentSession> findAllBySessionIdAndNotEnded(@Param("sessionId") UUID sessionId);
}

