package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 转移会话请求
 */
public record TransferSessionRequest(
    @NotNull(message = "目标客服ID不能为空")
    UUID targetAgentId,
    
    /**
     * 是否将原主要客服保留为支持客服
     * 默认为 false
     */
    Boolean keepAsSupport
) {
    public TransferSessionRequest {
        if (keepAsSupport == null) {
            keepAsSupport = false;
        }
    }
}

