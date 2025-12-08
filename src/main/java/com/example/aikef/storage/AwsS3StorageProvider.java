package com.example.aikef.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * AWS S3 文件存储实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class AwsS3StorageProvider implements StorageProvider {
    
    @Value("${storage.s3.access-key}")
    private String accessKey;
    
    @Value("${storage.s3.secret-key}")
    private String secretKey;
    
    @Value("${storage.s3.region:us-east-1}")
    private String region;
    
    @Value("${storage.s3.bucket}")
    private String bucket;
    
    @Value("${storage.s3.endpoint:}")
    private String endpoint;
    
    @Value("${storage.s3.public-url:}")
    private String publicUrl;
    
    @Value("${storage.s3.presigned-url-expiration:3600}")
    private int presignedUrlExpiration;
    
    private S3Client s3Client;
    private S3Presigner s3Presigner;
    
    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        var clientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        
        // 支持自定义 endpoint（如 MinIO、阿里云 OSS 等 S3 兼容存储）
        if (endpoint != null && !endpoint.isEmpty()) {
            URI endpointUri = URI.create(endpoint);
            clientBuilder.endpointOverride(endpointUri);
            presignerBuilder.endpointOverride(endpointUri);
        }
        
        s3Client = clientBuilder.build();
        s3Presigner = presignerBuilder.build();
        
        log.info("AWS S3 存储初始化完成: bucket={}, region={}", bucket, region);
    }
    
    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
    
    @Override
    public String getType() {
        return "S3";
    }
    
    @Override
    public String upload(String path, InputStream inputStream, String contentType, long size) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, size));
            
            log.info("S3 文件上传成功: bucket={}, key={}, size={}", bucket, path, size);
            
            return getUrl(path);
            
        } catch (Exception e) {
            log.error("S3 文件上传失败: bucket={}, key={}", bucket, path, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    public boolean delete(String path) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            
            log.info("S3 文件删除成功: bucket={}, key={}", bucket, path);
            return true;
            
        } catch (Exception e) {
            log.error("S3 文件删除失败: bucket={}, key={}", bucket, path, e);
            return false;
        }
    }
    
    @Override
    public boolean exists(String path) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build();
            
            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("S3 检查文件存在失败: bucket={}, key={}", bucket, path, e);
            return false;
        }
    }
    
    @Override
    public String getUrl(String path) {
        // 如果配置了公共 URL，直接返回
        if (publicUrl != null && !publicUrl.isEmpty()) {
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            return publicUrl + "/" + cleanPath;
        }
        
        // 否则生成预签名 URL
        return generatePresignedUrl(path);
    }
    
    /**
     * 生成预签名下载 URL
     */
    public String generatePresignedUrl(String path) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                .getObjectRequest(getRequest)
                .build();
        
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        
        return presignedRequest.url().toString();
    }
    
    @Override
    public InputStream download(String path) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build();
            
            return s3Client.getObject(getRequest);
            
        } catch (NoSuchKeyException e) {
            throw new RuntimeException("文件不存在: " + path);
        } catch (Exception e) {
            log.error("S3 文件下载失败: bucket={}, key={}", bucket, path, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }
}

