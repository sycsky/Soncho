package com.example.aikef.repository;

import com.example.aikef.model.ReadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadRecordRepository extends JpaRepository<ReadRecord, UUID> {

    /**
     * 查找指定会话和客服的已读记录
     */
    Optional<ReadRecord> findBySessionIdAndAgentId(UUID sessionId, UUID agentId);

    /**
     * 查找指定客服的所有已读记录
     */
    List<ReadRecord> findByAgentId(UUID agentId);

    /**
     * 批量查询客服在多个会话的已读记录
     */
    @Query("SELECT r FROM ReadRecord r WHERE r.agent.id = :agentId AND r.session.id IN :sessionIds")
    List<ReadRecord> findByAgentIdAndSessionIdIn(@Param("agentId") UUID agentId, 
                                                   @Param("sessionIds") List<UUID> sessionIds);
}
