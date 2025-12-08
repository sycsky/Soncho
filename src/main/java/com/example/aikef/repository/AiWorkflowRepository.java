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
     * 查找默认工作流
     */
    Optional<AiWorkflow> findByIsDefaultTrueAndEnabledTrue();

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
}

