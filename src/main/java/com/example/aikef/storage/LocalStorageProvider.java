package com.example.aikef.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageProvider implements StorageProvider {
    
    @Value("${storage.local.base-path:./uploads}")
    private String basePath;
    
    @Value("${storage.local.base-url:http://localhost:8080/api/v1/files}")
    private String baseUrl;
    
    @PostConstruct
    public void init() {
        try {
            Path uploadDir = Paths.get(basePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建本地存储目录: {}", uploadDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建本地存储目录失败", e);
            throw new RuntimeException("无法创建存储目录", e);
        }
    }
    
    @Override
    public String getType() {
        return "LOCAL";
    }
    
    @Override
    public String upload(String path, InputStream inputStream, String contentType, long size) {
        try {
            Path targetPath = Paths.get(basePath, path);
            
            // 确保父目录存在
            Files.createDirectories(targetPath.getParent());
            
            // 复制文件
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("本地文件上传成功: path={}, size={}", path, size);
            
            return getUrl(path);
            
        } catch (IOException e) {
            log.error("本地文件上传失败: path={}", path, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    public boolean delete(String path) {
        try {
            Path targetPath = Paths.get(basePath, path);
            boolean deleted = Files.deleteIfExists(targetPath);
            
            if (deleted) {
                log.info("本地文件删除成功: path={}", path);
            }
            
            return deleted;
            
        } catch (IOException e) {
            log.error("本地文件删除失败: path={}", path, e);
            return false;
        }
    }
    
    @Override
    public boolean exists(String path) {
        Path targetPath = Paths.get(basePath, path);
        return Files.exists(targetPath);
    }
    
    @Override
    public String getUrl(String path) {
        // 移除开头的斜杠
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        return baseUrl + "/" + cleanPath;
    }
    
    @Override
    public InputStream download(String path) {
        try {
            Path targetPath = Paths.get(basePath, path);
            
            if (!Files.exists(targetPath)) {
                throw new RuntimeException("文件不存在: " + path);
            }
            
            return Files.newInputStream(targetPath);
            
        } catch (IOException e) {
            log.error("本地文件下载失败: path={}", path, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }
    
    /**
     * 获取本地文件的绝对路径
     */
    public Path getAbsolutePath(String path) {
        return Paths.get(basePath, path);
    }
}

