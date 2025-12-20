package com.example.aikef.tool.repository;

import com.example.aikef.tool.model.ToolExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ToolExecutionRepository extends JpaRepository<ToolExecution, UUID> {

    Page<ToolExecution> findByTool_Id(UUID toolId, Pageable pageable);

    List<ToolExecution> findBySessionId(UUID sessionId);

    void deleteByTool_Id(UUID toolId);

    Page<ToolExecution> findByStatus(ToolExecution.ExecutionStatus status, Pageable pageable);

    @Query("SELECT te FROM ToolExecution te WHERE te.tool.id = :toolId AND te.createdAt >= :since ORDER BY te.createdAt DESC")
    List<ToolExecution> findRecentByToolId(UUID toolId, Instant since);

    @Query("SELECT COUNT(te) FROM ToolExecution te WHERE te.tool.id = :toolId AND te.status = :status")
    long countByToolIdAndStatus(UUID toolId, ToolExecution.ExecutionStatus status);

    @Query("SELECT AVG(te.durationMs) FROM ToolExecution te WHERE te.tool.id = :toolId AND te.status = 'SUCCESS'")
    Double getAverageDurationByToolId(UUID toolId);
}

