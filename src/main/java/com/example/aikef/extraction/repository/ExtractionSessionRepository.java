package com.example.aikef.extraction.repository;

import com.example.aikef.extraction.model.ExtractionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractionSessionRepository extends JpaRepository<ExtractionSession, UUID> {
    
    List<ExtractionSession> findBySchema_Id(UUID schemaId);
    
    List<ExtractionSession> findByStatus(ExtractionSession.SessionStatus status);
    
    List<ExtractionSession> findByCreatedBy(UUID createdBy);
    
    List<ExtractionSession> findByReferenceIdAndReferenceType(UUID referenceId, String referenceType);
}

