package com.example.aikef.tool.repository;

import com.example.aikef.tool.model.AiTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiToolRepository extends JpaRepository<AiTool, UUID> {

    Optional<AiTool> findByName(String name);

    boolean existsByName(String name);

    /**
     * 获取所有启用的工具（带 Schema）
     */
    @Query("SELECT t FROM AiTool t LEFT JOIN FETCH t.schema WHERE t.enabled = true ORDER BY t.sortOrder ASC")
    List<AiTool> findByEnabledTrueOrderBySortOrderAsc();

    List<AiTool> findByToolType(AiTool.ToolType toolType);

    List<AiTool> findByToolTypeAndEnabledTrue(AiTool.ToolType toolType);

    /**
     * 根据ID查找工具（带 Schema）
     */
    @Query("SELECT t FROM AiTool t LEFT JOIN FETCH t.schema WHERE t.id = :id")
    Optional<AiTool> findByIdWithSchema(UUID id);

    /**
     * 根据名称查找工具（带 Schema）
     */
    @Query("SELECT t FROM AiTool t LEFT JOIN FETCH t.schema WHERE t.name = :name")
    Optional<AiTool> findByNameWithSchema(String name);

    /**
     * 查找所有启用的工具（带 Schema，包括没有 schema 的工具）
     */
    @Query("SELECT t FROM AiTool t LEFT JOIN FETCH t.schema WHERE t.enabled = true ORDER BY t.sortOrder ASC")
    List<AiTool> findEnabledToolsWithSchema();

    @Query("SELECT t FROM AiTool t WHERE t.enabled = true AND " +
            "(LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<AiTool> searchByKeyword(String keyword);
}

