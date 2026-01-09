package com.example.aikef.repository;

import com.example.aikef.model.AiScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiScheduledTaskRepository extends JpaRepository<AiScheduledTask, UUID> {
    
    /**
     * 查找所有启用的任务，且下次运行时间在指定时间之前的
     */
    List<AiScheduledTask> findByEnabledTrueAndNextRunAtLessThanEqual(Instant time);
    
    /**
     * 查找指定工作流关联的任务
     */
    List<AiScheduledTask> findByWorkflowId(UUID workflowId);
}
