package com.example.aikef.repository;

import com.example.aikef.model.SessionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionCategoryRepository extends JpaRepository<SessionCategory, UUID> {

    /**
     * 根据名称查询分类
     */
    Optional<SessionCategory> findByName(String name);

    /**
     * 检查名称是否已存在
     */
    boolean existsByName(String name);

    /**
     * 查询所有启用的分类（按排序）
     */
    List<SessionCategory> findByEnabledTrueOrderBySortOrderAsc();

    /**
     * 查询所有分类（按排序）
     */
    List<SessionCategory> findAllByOrderBySortOrderAsc();

    /**
     * 根据创建人查询分类
     */
    List<SessionCategory> findByCreatedByAgent_IdOrderBySortOrderAsc(UUID agentId);
}

