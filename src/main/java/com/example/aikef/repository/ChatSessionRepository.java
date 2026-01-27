package com.example.aikef.repository;

import com.example.aikef.model.ChatSession;
import com.example.aikef.model.enums.SessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByPrimaryAgent_Id(UUID agentId);

    List<ChatSession> findByStatus(SessionStatus status);

    List<ChatSession> findByCustomer_Id(UUID customerId);

    List<ChatSession> findByLastActiveAtBefore(Instant cutoff);

    @Query("SELECT s FROM ChatSession s WHERE s.primaryAgent.id = :agentId OR :agentId MEMBER OF s.supportAgentIds")
    List<ChatSession> findByPrimaryAgentIdOrSupportAgentIdsContaining(@Param("agentId") UUID agentId);

    ChatSession findFirstByCustomer_IdOrderByLastActiveAtDesc(UUID customerId);

    long countByCreatedAtBetweenAndTenantId(java.time.Instant start, java.time.Instant end, String tenantId);

    List<ChatSession> findByCreatedAtBetweenAndTenantId(java.time.Instant start, java.time.Instant end, String tenantId);
}
