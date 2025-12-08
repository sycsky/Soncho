package com.example.aikef.controller;

import com.example.aikef.dto.UploadedFileDto;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import com.example.aikef.storage.FileUploadService;
import com.example.aikef.storage.LocalStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    
    private final FileUploadService fileUploadService;
    private final LocalStorageProvider localStorageProvider;
    
    @Value("${storage.local.base-path:./uploads}")
    private String localBasePath;
    
    public FileController(FileUploadService fileUploadService, 
                          @Autowired(required = false) LocalStorageProvider localStorageProvider) {
        this.fileUploadService = fileUploadService;
        this.localStorageProvider = localStorageProvider;
    }
    
    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    public UploadedFileDto uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "referenceId", required = false) UUID referenceId,
            @RequestParam(value = "referenceType", required = false) String referenceType,
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic,
            Authentication authentication) throws IOException {
        
        UUID uploaderId = null;
        String uploaderType = "SYSTEM";
        
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AgentPrincipal agent) {
                uploaderId = agent.getId();
                uploaderType = "AGENT";
            } else if (principal instanceof CustomerPrincipal customer) {
                uploaderId = customer.getId();
                uploaderType = "CUSTOMER";
            }
        }
        
        return fileUploadService.uploadFile(
                file, uploaderId, uploaderType, referenceId, referenceType, isPublic);
    }
    
    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    public List<UploadedFileDto> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "referenceId", required = false) UUID referenceId,
            @RequestParam(value = "referenceType", required = false) String referenceType,
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic,
            Authentication authentication) throws IOException {
        
        UUID uploaderId = null;
        String uploaderType = "SYSTEM";
        
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AgentPrincipal agent) {
                uploaderId = agent.getId();
                uploaderType = "AGENT";
            } else if (principal instanceof CustomerPrincipal customer) {
                uploaderId = customer.getId();
                uploaderType = "CUSTOMER";
            }
        }
        
        return fileUploadService.uploadFiles(
                files, uploaderId, uploaderType, referenceId, referenceType, isPublic);
    }
    
    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    public UploadedFileDto getFile(@PathVariable UUID fileId) {
        return fileUploadService.getFile(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在: " + fileId));
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {
        UploadedFileDto fileInfo = fileUploadService.getFile(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在: " + fileId));
        
        InputStream inputStream = fileUploadService.downloadFile(fileId);
        
        String encodedFilename = URLEncoder.encode(fileInfo.originalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.fileSize()))
                .body(new InputStreamResource(inputStream));
    }
    
    /**
     * 本地文件直接访问（用于本地存储的图片预览等）
     * 路径格式: /api/v1/files/image/2024/11/29/xxx.jpg
     */
    @GetMapping("/{category}/{year}/{month}/{day}/{filename}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String category,
            @PathVariable String year,
            @PathVariable String month,
            @PathVariable String day,
            @PathVariable String filename) throws IOException {
        
        String storagePath = String.format("%s/%s/%s/%s/%s", category, year, month, day, filename);
        
        Path filePath;
        if (localStorageProvider != null) {
            filePath = localStorageProvider.getAbsolutePath(storagePath);
        } else {
            filePath = Paths.get(localBasePath, storagePath);
        }
        
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        InputStream inputStream = Files.newInputStream(filePath);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(inputStream));
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public void deleteFile(@PathVariable UUID fileId, Authentication authentication) {
        boolean deleted = fileUploadService.deleteFile(fileId);
        if (!deleted) {
            throw new IllegalArgumentException("文件删除失败: " + fileId);
        }
    }
    
    /**
     * 获取我上传的文件列表
     */
    @GetMapping("/my")
    public Page<UploadedFileDto> getMyFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UUID uploaderId = null;
        String uploaderType = "SYSTEM";
        
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AgentPrincipal agent) {
                uploaderId = agent.getId();
                uploaderType = "AGENT";
            } else if (principal instanceof CustomerPrincipal customer) {
                uploaderId = customer.getId();
                uploaderType = "CUSTOMER";
            }
        }
        
        if (uploaderId == null) {
            throw new IllegalStateException("未登录");
        }
        
        return fileUploadService.getFilesByUploader(
                uploaderId, uploaderType, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
    
    /**
     * 获取关联业务的文件列表
     */
    @GetMapping("/reference/{referenceType}/{referenceId}")
    public List<UploadedFileDto> getFilesByReference(
            @PathVariable String referenceType,
            @PathVariable UUID referenceId) {
        
        return fileUploadService.getFilesByReference(referenceId, referenceType);
    }
    
    /**
     * 刷新文件URL
     */
    @PostMapping("/{fileId}/refresh-url")
    public Map<String, String> refreshUrl(@PathVariable UUID fileId) {
        String newUrl = fileUploadService.refreshUrl(fileId);
        return Map.of("url", newUrl);
    }
    
    /**
     * 获取上传限制信息
     */
    @GetMapping("/config")
    public Map<String, Object> getUploadConfig() {
        return Map.of(
                "maxFileSize", fileUploadService.getMaxFileSize(),
                "maxFileSizeFormatted", formatFileSize(fileUploadService.getMaxFileSize())
        );
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
