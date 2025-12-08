package com.example.aikef.dto;

import java.util.UUID;

/**
 * Webhook 消息处理响应
 */
public record WebhookMessageResponse(
        /**
         * 是否成功
         */
        boolean success,

        /**
         * 消息 ID
         */
        UUID messageId,

        /**
         * 会话 ID
         */
        UUID sessionId,

        /**
         * 是否为新建会话
         */
        boolean newSession,

        /**
         * 客户 ID
         */
        UUID customerId,

        /**
         * 错误信息（失败时）
         */
        String errorMessage
) {
    public static WebhookMessageResponse success(UUID messageId, UUID sessionId, UUID customerId, boolean newSession) {
        return new WebhookMessageResponse(true, messageId, sessionId, newSession, customerId, null);
    }

    public static WebhookMessageResponse error(String errorMessage) {
        return new WebhookMessageResponse(false, null, null, false, null, errorMessage);
    }
}

