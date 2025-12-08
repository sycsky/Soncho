package com.example.aikef.repository;

import com.example.aikef.model.UploadedFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    
    /**
     * 根据存储路径查找
     */
    Optional<UploadedFile> findByStoragePath(String storagePath);
    
    /**
     * 根据MD5哈希查找（用于去重）
     */
    Optional<UploadedFile> findByMd5Hash(String md5Hash);
    
    /**
     * 根据上传者查找
     */
    Page<UploadedFile> findByUploaderIdAndUploaderType(UUID uploaderId, String uploaderType, Pageable pageable);
    
    /**
     * 根据分类查找
     */
    Page<UploadedFile> findByCategory(UploadedFile.FileCategory category, Pageable pageable);
    
    /**
     * 根据关联业务查找
     */
    List<UploadedFile> findByReferenceIdAndReferenceType(UUID referenceId, String referenceType);
    
    /**
     * 根据上传者和分类查找
     */
    Page<UploadedFile> findByUploaderIdAndCategory(UUID uploaderId, UploadedFile.FileCategory category, Pageable pageable);
}

