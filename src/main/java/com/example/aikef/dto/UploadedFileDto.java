package com.example.aikef.dto;

import com.example.aikef.model.UploadedFile;

import java.util.UUID;

/**
 * 上传文件DTO
 */
public record UploadedFileDto(
        UUID id,
        String originalName,
        String url,
        String contentType,
        Long fileSize,
        String extension,
        String storageType,
        UploadedFile.FileCategory category,
        UUID uploaderId,
        String uploaderType,
        UUID referenceId,
        String referenceType,
        Boolean isPublic,
        String createdAt
) {
}

