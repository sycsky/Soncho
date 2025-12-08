package com.example.aikef.repository;

import com.example.aikef.model.ExternalPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalPlatformRepository extends JpaRepository<ExternalPlatform, UUID> {
    
    Optional<ExternalPlatform> findByName(String name);
    
    Optional<ExternalPlatform> findByNameAndEnabledTrue(String name);
    
    boolean existsByName(String name);
}

