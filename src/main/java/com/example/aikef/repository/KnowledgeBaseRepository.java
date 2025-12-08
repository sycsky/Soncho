package com.example.aikef.repository;

import com.example.aikef.model.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {
    
    List<KnowledgeBase> findByEnabledTrue();
    
    Optional<KnowledgeBase> findByIndexName(String indexName);
    
    boolean existsByName(String name);
    
    List<KnowledgeBase> findByCreatedByAgent_Id(UUID agentId);
}


