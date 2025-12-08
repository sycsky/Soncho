package com.example.aikef.repository;

import com.example.aikef.model.KnowledgeDocument;
import com.example.aikef.model.KnowledgeDocument.ProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    
    Page<KnowledgeDocument> findByKnowledgeBase_Id(UUID knowledgeBaseId, Pageable pageable);
    
    List<KnowledgeDocument> findByKnowledgeBase_Id(UUID knowledgeBaseId);
    
    List<KnowledgeDocument> findByStatus(ProcessStatus status);
    
    List<KnowledgeDocument> findByKnowledgeBase_IdAndStatus(UUID knowledgeBaseId, ProcessStatus status);
    
    int countByKnowledgeBase_Id(UUID knowledgeBaseId);
    
    @Query("SELECT SUM(d.chunkCount) FROM KnowledgeDocument d WHERE d.knowledgeBase.id = :kbId AND d.status = 'COMPLETED'")
    Integer getTotalChunkCount(@Param("kbId") UUID knowledgeBaseId);
    
    void deleteByKnowledgeBase_Id(UUID knowledgeBaseId);
}


