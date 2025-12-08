package com.example.aikef.repository;

import com.example.aikef.model.ExternalSessionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalSessionMappingRepository extends JpaRepository<ExternalSessionMapping, UUID> {
    
    /**
     * 根据平台名称和外部线程 ID 查找映射
     */
    @Query("SELECT m FROM ExternalSessionMapping m " +
           "JOIN FETCH m.platform p " +
           "JOIN FETCH m.session s " +
           "JOIN FETCH m.customer c " +
           "WHERE p.name = :platformName AND m.externalThreadId = :threadId AND m.active = true")
    Optional<ExternalSessionMapping> findByPlatformNameAndThreadId(
            @Param("platformName") String platformName,
            @Param("threadId") String threadId);
    
    /**
     * 根据平台 ID 和外部线程 ID 查找映射
     */
    @Query("SELECT m FROM ExternalSessionMapping m " +
           "JOIN FETCH m.platform p " +
           "JOIN FETCH m.session s " +
           "JOIN FETCH m.customer c " +
           "WHERE p.id = :platformId AND m.externalThreadId = :threadId AND m.active = true")
    Optional<ExternalSessionMapping> findByPlatformIdAndThreadId(
            @Param("platformId") UUID platformId,
            @Param("threadId") String threadId);
    
    /**
     * 根据会话 ID 查找映射
     */
    @Query("SELECT m FROM ExternalSessionMapping m " +
           "JOIN FETCH m.platform p " +
           "WHERE m.session.id = :sessionId AND m.active = true")
    Optional<ExternalSessionMapping> findBySessionId(@Param("sessionId") UUID sessionId);
    
    /**
     * 检查映射是否存在
     */
    @Query("SELECT COUNT(m) > 0 FROM ExternalSessionMapping m " +
           "WHERE m.platform.name = :platformName AND m.externalThreadId = :threadId AND m.active = true")
    boolean existsByPlatformNameAndThreadId(
            @Param("platformName") String platformName,
            @Param("threadId") String threadId);
}

