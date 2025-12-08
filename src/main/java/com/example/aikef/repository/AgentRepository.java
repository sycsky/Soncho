package com.example.aikef.repository;

import com.example.aikef.model.Agent;
import com.example.aikef.model.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID>, JpaSpecificationExecutor<Agent> {

    Optional<Agent> findByEmail(String email);
    
    Optional<Agent> findByEmailIgnoreCase(String email);
    
    List<Agent> findByStatus(AgentStatus status);

    /**
     * 根据ID查询Agent并预加载Role（避免懒加载问题）
     */
    @Query("SELECT a FROM Agent a LEFT JOIN FETCH a.role WHERE a.id = :id")
    Optional<Agent> findByIdWithRole(@Param("id") UUID id);
}
