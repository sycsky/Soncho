package com.example.aikef.repository;

import com.example.aikef.model.Agent;
import com.example.aikef.model.SessionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionGroupRepository extends JpaRepository<SessionGroup, UUID> {

    /**
     * 查询客服的所有分组
     */
    List<SessionGroup> findByAgentOrderBySortOrderAsc(Agent agent);

    /**
     * 查询客服的所有分组（按 agentId）
     */
    List<SessionGroup> findByAgent_IdOrderBySortOrderAsc(UUID agentId);

    /**
     * 查询客服的系统分组
     */
    List<SessionGroup> findByAgentAndSystemTrue(Agent agent);

    /**
     * 查询客服的指定名称的分组
     */
    Optional<SessionGroup> findByAgentAndName(Agent agent, String name);

    /**
     * 检查客服是否有指定名称的分组
     */
    boolean existsByAgentAndName(Agent agent, String name);
}
