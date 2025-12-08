package com.example.aikef.repository;

import com.example.aikef.model.Agent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.SessionGroup;
import com.example.aikef.model.SessionGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionGroupMappingRepository extends JpaRepository<SessionGroupMapping, UUID> {

    /**
     * 查询某个session的所有分组映射
     */
    List<SessionGroupMapping> findBySession(ChatSession session);

    /**
     * 查询某个客服对某个session的分组映射
     */
    Optional<SessionGroupMapping> findBySessionAndAgent(ChatSession session, Agent agent);

    /**
     * 查询某个客服对某个session的分组映射（通过ID）
     */
    @Query("SELECT m FROM SessionGroupMapping m WHERE m.session.id = :sessionId AND m.agent.id = :agentId")
    Optional<SessionGroupMapping> findBySessionIdAndAgentId(@Param("sessionId") UUID sessionId, @Param("agentId") UUID agentId);

    /**
     * 查询某个分组下的所有映射
     */
    List<SessionGroupMapping> findBySessionGroup(SessionGroup sessionGroup);

    /**
     * 查询某个分组下的所有映射（通过ID）
     */
    @Query("SELECT m FROM SessionGroupMapping m WHERE m.sessionGroup.id = :groupId AND m.agent.id = :agentId")
    List<SessionGroupMapping> findBySessionGroupIdAndAgentId(@Param("groupId") UUID groupId, @Param("agentId") UUID agentId);

    /**
     * 查询某个客服的所有分组映射
     */
    List<SessionGroupMapping> findByAgent(Agent agent);

    /**
     * 删除某个客服对某个session的映射
     */
    void deleteBySessionAndAgent(ChatSession session, Agent agent);

    /**
     * 删除某个session的所有映射
     */
    void deleteBySession(ChatSession session);
}
