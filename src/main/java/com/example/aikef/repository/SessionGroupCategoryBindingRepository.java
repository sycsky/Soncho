package com.example.aikef.repository;

import com.example.aikef.model.SessionGroupCategoryBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionGroupCategoryBindingRepository extends JpaRepository<SessionGroupCategoryBinding, UUID> {

    /**
     * 根据分组查询所有绑定的分类
     */
    List<SessionGroupCategoryBinding> findBySessionGroup_Id(UUID sessionGroupId);

    /**
     * 根据客服查询所有绑定关系
     */
    List<SessionGroupCategoryBinding> findByAgent_Id(UUID agentId);

    /**
     * 根据分类查询所有绑定关系
     */
    List<SessionGroupCategoryBinding> findByCategory_Id(UUID categoryId);

    /**
     * 检查某个客服下某个分类是否已被绑定
     */
    boolean existsByAgent_IdAndCategory_Id(UUID agentId, UUID categoryId);

    /**
     * 根据客服和分类查询绑定（用于查找分类对应的分组）
     */
    Optional<SessionGroupCategoryBinding> findByAgent_IdAndCategory_Id(UUID agentId, UUID categoryId);

    /**
     * 检查某个分组是否绑定了某个分类
     */
    boolean existsBySessionGroup_IdAndCategory_Id(UUID sessionGroupId, UUID categoryId);

    /**
     * 删除某个分组的所有绑定
     */
    void deleteBySessionGroup_Id(UUID sessionGroupId);

    /**
     * 删除某个分类的所有绑定
     */
    void deleteByCategory_Id(UUID categoryId);

    /**
     * 根据分组和分类删除绑定
     */
    void deleteBySessionGroup_IdAndCategory_Id(UUID sessionGroupId, UUID categoryId);

    /**
     * 根据客服ID和分类ID查找绑定的分组ID
     */
    @Query("SELECT b.sessionGroup.id FROM SessionGroupCategoryBinding b WHERE b.agent.id = :agentId AND b.category.id = :categoryId")
    Optional<UUID> findGroupIdByAgentIdAndCategoryId(@Param("agentId") UUID agentId, @Param("categoryId") UUID categoryId);
}

