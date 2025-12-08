package com.example.aikef.repository;

import com.example.aikef.model.WorkflowExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 工作流执行日志 Repository
 */
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, UUID> {

    /**
     * 根据工作流ID查找执行日志
     */
    Page<WorkflowExecutionLog> findByWorkflow_IdOrderByCreatedAtDesc(UUID workflowId, Pageable pageable);

    /**
     * 根据会话ID查找执行日志
     */
    List<WorkflowExecutionLog> findBySession_IdOrderByCreatedAtDesc(UUID sessionId);

    /**
     * 根据状态查找执行日志
     */
    Page<WorkflowExecutionLog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * 统计工作流成功执行次数
     */
    long countByWorkflow_IdAndStatus(UUID workflowId, String status);

    /**
     * 统计指定时间范围内的执行次数
     */
    @Query("SELECT COUNT(l) FROM WorkflowExecutionLog l " +
           "WHERE l.workflow.id = :workflowId " +
           "AND l.createdAt BETWEEN :startTime AND :endTime")
    long countByWorkflowIdAndTimeRange(
            @Param("workflowId") UUID workflowId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * 获取工作流平均执行时间
     */
    @Query("SELECT AVG(l.durationMs) FROM WorkflowExecutionLog l " +
           "WHERE l.workflow.id = :workflowId AND l.status = 'SUCCESS'")
    Double getAverageDuration(@Param("workflowId") UUID workflowId);
}

