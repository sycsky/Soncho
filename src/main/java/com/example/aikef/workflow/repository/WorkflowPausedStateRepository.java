package com.example.aikef.workflow.repository;

import com.example.aikef.workflow.model.WorkflowPausedState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowPausedStateRepository extends JpaRepository<WorkflowPausedState, UUID> {

    /**
     * 查找会话的未完成暂停状态（等待用户输入）
     */
    @Query("SELECT p FROM WorkflowPausedState p WHERE p.sessionId = :sessionId " +
           "AND p.status = 'WAITING_USER_INPUT' AND p.expiredAt > :now ORDER BY p.createdAt DESC")
    List<WorkflowPausedState> findPendingBySessionId(UUID sessionId, Instant now);

    /**
     * 查找会话最新的未完成暂停状态
     */
    default Optional<WorkflowPausedState> findLatestPendingBySessionId(UUID sessionId) {
        List<WorkflowPausedState> list = findPendingBySessionId(sessionId, Instant.now());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 查找工作流的所有暂停状态
     */
    List<WorkflowPausedState> findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId);

    /**
     * 取消会话的所有未完成暂停状态
     */
    @Modifying
    @Query("UPDATE WorkflowPausedState p SET p.status = 'CANCELLED', p.updatedAt = :now " +
           "WHERE p.sessionId = :sessionId AND p.status = 'WAITING_USER_INPUT'")
    int cancelAllPendingBySessionId(UUID sessionId, Instant now);

    /**
     * 标记过期的暂停状态
     */
    @Modifying
    @Query("UPDATE WorkflowPausedState p SET p.status = 'EXPIRED', p.updatedAt = :now " +
           "WHERE p.status = 'WAITING_USER_INPUT' AND p.expiredAt < :now")
    int markExpiredStates(Instant now);

    /**
     * 删除旧的已完成/已取消/已过期状态
     */
    @Modifying
    @Query("DELETE FROM WorkflowPausedState p WHERE p.status IN ('COMPLETED', 'CANCELLED', 'EXPIRED') " +
           "AND p.updatedAt < :before")
    int deleteOldStates(Instant before);
}

