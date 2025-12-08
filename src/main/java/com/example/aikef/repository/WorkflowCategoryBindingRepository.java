package com.example.aikef.repository;

import com.example.aikef.model.WorkflowCategoryBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 工作流分类绑定 Repository
 * 
 * 关系说明：
 * - 一个工作流可以绑定多个分类
 * - 一个分类只能绑定一个工作流
 */
public interface WorkflowCategoryBindingRepository extends JpaRepository<WorkflowCategoryBinding, UUID> {

    /**
     * 根据分类ID查找绑定（一个分类只有一个绑定）
     */
    Optional<WorkflowCategoryBinding> findByCategory_Id(UUID categoryId);

    /**
     * 根据分类ID查找绑定的工作流
     */
    @Query("SELECT b FROM WorkflowCategoryBinding b " +
           "JOIN FETCH b.workflow w " +
           "WHERE b.category.id = :categoryId " +
           "AND w.enabled = true")
    Optional<WorkflowCategoryBinding> findByCategoryIdWithWorkflow(@Param("categoryId") UUID categoryId);

    /**
     * 根据工作流ID查找所有绑定
     */
    List<WorkflowCategoryBinding> findByWorkflow_Id(UUID workflowId);

    /**
     * 根据工作流ID查找所有绑定（带分类信息）
     */
    @Query("SELECT b FROM WorkflowCategoryBinding b " +
           "JOIN FETCH b.category " +
           "WHERE b.workflow.id = :workflowId " +
           "ORDER BY b.priority ASC")
    List<WorkflowCategoryBinding> findByWorkflowIdWithCategory(@Param("workflowId") UUID workflowId);

    /**
     * 删除工作流的所有绑定
     * flushAutomatically: 执行后立即刷新，防止后续插入时键冲突
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM WorkflowCategoryBinding b WHERE b.workflow.id = :workflowId")
    void deleteByWorkflow_Id(@Param("workflowId") UUID workflowId);

    /**
     * 检查分类是否已被绑定
     */
    boolean existsByCategory_Id(UUID categoryId);

    /**
     * 检查分类是否被其他工作流绑定（排除指定工作流）
     */
    @Query("SELECT COUNT(b) > 0 FROM WorkflowCategoryBinding b " +
           "WHERE b.category.id = :categoryId " +
           "AND b.workflow.id != :workflowId")
    boolean existsByCategoryIdAndWorkflowIdNot(@Param("categoryId") UUID categoryId, 
                                                @Param("workflowId") UUID workflowId);

    /**
     * 获取所有已绑定的分类ID
     */
    @Query("SELECT b.category.id FROM WorkflowCategoryBinding b")
    List<UUID> findAllBoundCategoryIds();

    /**
     * 获取所有已绑定的分类ID（排除指定工作流）
     */
    @Query("SELECT b.category.id FROM WorkflowCategoryBinding b WHERE b.workflow.id != :workflowId")
    List<UUID> findBoundCategoryIdsExcludingWorkflow(@Param("workflowId") UUID workflowId);
}

