package com.example.aikef.extraction.repository;

import com.example.aikef.extraction.model.ExtractionSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionSchemaRepository extends JpaRepository<ExtractionSchema, UUID> {
    
    List<ExtractionSchema> findByEnabledTrue();
    
    Optional<ExtractionSchema> findByName(String name);
    
    boolean existsByName(String name);
}

