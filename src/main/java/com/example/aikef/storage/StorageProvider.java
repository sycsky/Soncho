package com.example.aikef.storage;

import java.io.InputStream;

/**
 * 文件存储提供者抽象接口
 * 定义统一的文件存储操作，支持切换不同存储实现
 */
public interface StorageProvider {
    
    /**
     * 获取存储类型标识
     */
    String getType();
    
    /**
     * 上传文件
     * @param path 存储路径（相对路径）
     * @param inputStream 文件输入流
     * @param contentType 文件MIME类型
     * @param size 文件大小
     * @return 文件访问URL
     */
    String upload(String path, InputStream inputStream, String contentType, long size);
    
    /**
     * 删除文件
     * @param path 存储路径
     * @return 是否删除成功
     */
    boolean delete(String path);
    
    /**
     * 检查文件是否存在
     * @param path 存储路径
     * @return 是否存在
     */
    boolean exists(String path);
    
    /**
     * 获取文件访问URL
     * @param path 存储路径
     * @return 访问URL
     */
    String getUrl(String path);
    
    /**
     * 获取文件输入流（用于下载）
     * @param path 存储路径
     * @return 文件输入流
     */
    InputStream download(String path);
}

