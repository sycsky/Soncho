package com.example.aikef.dto.request;

import java.util.UUID;

/**
 * 文件上传请求参数
 */
public record FileUploadRequest(
        /**
         * 关联业务ID（可选）
         */
        UUID referenceId,
        
        /**
         * 关联业务类型（可选，如 MESSAGE, KNOWLEDGE_DOCUMENT 等）
         */
        String referenceType,
        
        /**
         * 是否公开访问
         */
        Boolean isPublic
) {
}

