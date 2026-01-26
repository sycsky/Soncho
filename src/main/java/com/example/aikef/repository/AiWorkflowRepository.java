package com.example.aikef.repository;

import com.example.aikef.model.AiWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 工作流 Repository
 */
public interface AiWorkflowRepository extends JpaRepository<AiWorkflow, UUID> {

    /**
     * 根据名称查找工作流
     */
    Optional<AiWorkflow> findByName(String name);

    /**
     * 检查名称是否已存在
     */
    boolean existsByName(String name);

    /**
     * 查找所有启用的工作流
     */
    List<AiWorkflow> findByEnabledTrueOrderByCreatedAtDesc();

    /**
     * 查找默认工作流（单个）
     */
    Optional<AiWorkflow> findByIsDefaultTrueAndEnabledTrue();

    /**
     * 查找所有默认工作流
     */
    List<AiWorkflow> findByIsDefaultTrue();

    /**
     * 根据触发类型查找启用的工作流
     */
    List<AiWorkflow> findByTriggerTypeAndEnabledTrueOrderByCreatedAtDesc(String triggerType);

    /**
     * 查找指定创建者的工作流
     */
    List<AiWorkflow> findByCreatedByAgent_IdOrderByCreatedAtDesc(UUID agentId);

    /**
     * 查找所有工作流（按创建时间倒序）
     */
    List<AiWorkflow> findAllByOrderByCreatedAtDesc();

    /**
     * 根据分类ID查找匹配的工作流
     * 在 triggerConfig JSON 中搜索分类ID
     */
    @Query("SELECT w FROM AiWorkflow w WHERE w.enabled = true " +
           "AND w.triggerType = 'CATEGORY' " +
           "AND w.triggerConfig LIKE %:categoryId%")
    List<AiWorkflow> findByCategoryId(@Param("categoryId") String categoryId);

    /**
     * 查找系统默认和指定租户的默认工作流 (忽略租户过滤器)
     */
    @Query(value = "SELECT * FROM ai_workflows WHERE is_default = 1 AND (tenant_id IS NULL OR tenant_id = :tenantId)", nativeQuery = true)
    List<AiWorkflow> findSystemAndTenantDefaultWorkflows(@Param("tenantId") String tenantId);
}

