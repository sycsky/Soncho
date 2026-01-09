package com.example.aikef.repository;

import com.example.aikef.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * 根据事件名称查找
     */
    Optional<Event> findByName(String name);

    /**
     * 检查事件名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 查询所有启用的事件（按排序顺序）
     */
    @Query("SELECT e FROM Event e WHERE e.enabled = true ORDER BY e.sortOrder ASC, e.createdAt ASC")
    List<Event> findByEnabledTrueOrderBySortOrder();

    /**
     * 根据工作流ID查找所有事件
     */
    List<Event> findByWorkflow_Id(UUID workflowId);
}





