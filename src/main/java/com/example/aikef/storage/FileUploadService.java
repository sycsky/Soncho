package com.example.aikef.storage;

import com.example.aikef.dto.UploadedFileDto;
import com.example.aikef.model.UploadedFile;
import com.example.aikef.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    
    private final StorageProvider storageProvider;
    private final UploadedFileRepository fileRepository;
    
    @Value("${storage.max-file-size:10485760}")  // 默认 10MB
    private long maxFileSize;
    
    @Value("${storage.allowed-image-types:image/jpeg,image/png,image/gif,image/webp,image/svg+xml}")
    private String allowedImageTypes;
    
    @Value("${storage.allowed-document-types:application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/plain,text/csv}")
    private String allowedDocumentTypes;
    
    @Value("${storage.allowed-video-types:video/mp4,video/webm,video/quicktime}")
    private String allowedVideoTypes;
    
    @Value("${storage.allowed-audio-types:audio/mpeg,audio/wav,audio/ogg}")
    private String allowedAudioTypes;
    
    /**
     * 上传文件
     */
    @Transactional
    public UploadedFileDto uploadFile(MultipartFile file, 
                                       UUID uploaderId, 
                                       String uploaderType,
                                       UUID referenceId,
                                       String referenceType,
                                       Boolean isPublic) throws IOException {
        
        // 1. 验证文件
        validateFile(file);
        
        // 2. 获取文件信息
        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        String extension = getFileExtension(originalName);
        UploadedFile.FileCategory category = determineCategory(contentType);
        
        // 3. 计算MD5（可选，用于去重）
        String md5Hash = calculateMD5(file.getInputStream());
        
        // 4. 检查是否已存在相同文件（可选去重逻辑）
        // Optional<UploadedFile> existing = fileRepository.findByMd5Hash(md5Hash);
        // if (existing.isPresent()) { return toDto(existing.get()); }
        
        // 5. 生成存储路径
        String storagePath = generateStoragePath(category, extension);
        
        // 6. 上传到存储
        String url = storageProvider.upload(storagePath, file.getInputStream(), contentType, fileSize);
        
        // 7. 保存文件记录
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalName(originalName);
        uploadedFile.setStoragePath(storagePath);
        uploadedFile.setUrl(url);
        uploadedFile.setContentType(contentType);
        uploadedFile.setFileSize(fileSize);
        uploadedFile.setExtension(extension);
        uploadedFile.setStorageType(storageProvider.getType());
        uploadedFile.setCategory(category);
        uploadedFile.setUploaderId(uploaderId);
        uploadedFile.setUploaderType(uploaderType);
        uploadedFile.setReferenceId(referenceId);
        uploadedFile.setReferenceType(referenceType);
        uploadedFile.setMd5Hash(md5Hash);
        uploadedFile.setIsPublic(isPublic != null ? isPublic : false);
        
        UploadedFile saved = fileRepository.save(uploadedFile);
        
        log.info("文件上传成功: id={}, name={}, size={}, storage={}", 
                saved.getId(), originalName, fileSize, storageProvider.getType());
        
        return toDto(saved);
    }
    
    /**
     * 批量上传文件
     */
    @Transactional
    public List<UploadedFileDto> uploadFiles(List<MultipartFile> files,
                                              UUID uploaderId,
                                              String uploaderType,
                                              UUID referenceId,
                                              String referenceType,
                                              Boolean isPublic) throws IOException {
        List<UploadedFileDto> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            results.add(uploadFile(file, uploaderId, uploaderType, referenceId, referenceType, isPublic));
        }
        
        return results;
    }
    
    /**
     * 获取文件信息
     */
    public Optional<UploadedFileDto> getFile(UUID fileId) {
        return fileRepository.findById(fileId).map(this::toDto);
    }
    
    /**
     * 获取文件下载流
     */
    public InputStream downloadFile(UUID fileId) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + fileId));
        
        return storageProvider.download(file.getStoragePath());
    }
    
    /**
     * 删除文件
     */
    @Transactional
    public boolean deleteFile(UUID fileId) {
        Optional<UploadedFile> fileOpt = fileRepository.findById(fileId);
        
        if (fileOpt.isEmpty()) {
            return false;
        }
        
        UploadedFile file = fileOpt.get();
        
        // 从存储中删除
        boolean deleted = storageProvider.delete(file.getStoragePath());
        
        if (deleted) {
            // 删除数据库记录
            fileRepository.delete(file);
            log.info("文件删除成功: id={}, path={}", fileId, file.getStoragePath());
        }
        
        return deleted;
    }
    
    /**
     * 获取上传者的文件列表
     */
    public Page<UploadedFileDto> getFilesByUploader(UUID uploaderId, String uploaderType, Pageable pageable) {
        return fileRepository.findByUploaderIdAndUploaderType(uploaderId, uploaderType, pageable)
                .map(this::toDto);
    }
    
    /**
     * 获取关联业务的文件列表
     */
    public List<UploadedFileDto> getFilesByReference(UUID referenceId, String referenceType) {
        return fileRepository.findByReferenceIdAndReferenceType(referenceId, referenceType)
                .stream()
                .map(this::toDto)
                .toList();
    }
    
    /**
     * 刷新文件URL（用于预签名URL过期后刷新）
     */
    public String refreshUrl(UUID fileId) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + fileId));
        
        String newUrl = storageProvider.getUrl(file.getStoragePath());
        
        // 更新数据库中的URL
        file.setUrl(newUrl);
        fileRepository.save(file);
        
        return newUrl;
    }
    
    /**
     * 获取最大文件大小限制
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    // ==================== 私有方法 ====================
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("文件大小超过限制: %d bytes (最大: %d bytes)", file.getSize(), maxFileSize));
        }
        
        String contentType = file.getContentType();
        if (!isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }
    }
    
    private boolean isAllowedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        Set<String> allowedTypes = new HashSet<>();
        allowedTypes.addAll(Arrays.asList(allowedImageTypes.split(",")));
        allowedTypes.addAll(Arrays.asList(allowedDocumentTypes.split(",")));
        allowedTypes.addAll(Arrays.asList(allowedVideoTypes.split(",")));
        allowedTypes.addAll(Arrays.asList(allowedAudioTypes.split(",")));
        
        return allowedTypes.contains(contentType.toLowerCase());
    }
    
    private UploadedFile.FileCategory determineCategory(String contentType) {
        if (contentType == null) {
            return UploadedFile.FileCategory.OTHER;
        }
        
        if (contentType.startsWith("image/")) {
            return UploadedFile.FileCategory.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return UploadedFile.FileCategory.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return UploadedFile.FileCategory.AUDIO;
        } else if (isDocumentType(contentType)) {
            return UploadedFile.FileCategory.DOCUMENT;
        }
        
        return UploadedFile.FileCategory.OTHER;
    }
    
    private boolean isDocumentType(String contentType) {
        return allowedDocumentTypes.contains(contentType);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private String generateStoragePath(UploadedFile.FileCategory category, String extension) {
        // 按日期分目录: category/yyyy/MM/dd/uuid.ext
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID().toString();
        
        if (extension != null && !extension.isEmpty()) {
            filename = filename + "." + extension;
        }
        
        return String.format("%s/%s/%s", category.name().toLowerCase(), datePath, filename);
    }
    
    private String calculateMD5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            
            inputStream.mark(Integer.MAX_VALUE);
            while ((read = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            inputStream.reset();
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("计算文件MD5失败", e);
            return null;
        }
    }
    
    private UploadedFileDto toDto(UploadedFile file) {
        return new UploadedFileDto(
                file.getId(),
                file.getOriginalName(),
                file.getUrl(),
                file.getContentType(),
                file.getFileSize(),
                file.getExtension(),
                file.getStorageType(),
                file.getCategory(),
                file.getUploaderId(),
                file.getUploaderType(),
                file.getReferenceId(),
                file.getReferenceType(),
                file.getIsPublic(),
                file.getCreatedAt() != null ? file.getCreatedAt().toString() : null
        );
    }
}

