package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 第三方平台 Webhook 消息请求
 */
public record WebhookMessageRequest(
        /**
         * 第三方平台的会话/线程 ID（必填）
         * 用于标识和关联会话
         */
        @NotBlank(message = "threadId 不能为空")
        String threadId,

        /**
         * 消息内容（必填）
         */
        String content,

        /**
         * 消息类型（可选，默认 text）
         * 支持: text, image, file, audio, video
         */
        String messageType,

        /**
         * 外部用户 ID（可选，如微信 openId）
         */
        String externalUserId,

        /**
         * 外部用户名称（可选）
         */
        String userName,

        /**
         * 用户邮箱（可选，用于创建客户）
         */
        String email,

        /**
         * 用户手机号（可选，用于创建客户）
         */
        String phone,

        /**
         * 会话分类 ID（可选，用于新建会话时指定分类）
         */
        String categoryId,

        /**
         * 附件 URL（可选，用于图片/文件消息）
         */
        String attachmentUrl,

        /**
         * 附件名称（可选）
         */
        String attachmentName,

        /**
         * 消息时间戳（可选，毫秒）
         */
        Long timestamp,

        /**
         * 用户使用的语言代码（可选，如 zh-TW, en, ja）
         * 如果提供，将设置为会话的客户语言
         */
        String language,

        /**
         * 额外元数据（可选）
         */
        Map<String, Object> metadata
) {
    /**
     * 获取消息类型，默认为 text
     */
    public String getMessageTypeOrDefault() {
        return messageType != null ? messageType : "text";
    }

    /**
     * 获取用户名，如果为空则使用外部用户 ID
     */
    public String getUserNameOrDefault() {
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        if (externalUserId != null && !externalUserId.isBlank()) {
            return "用户_" + externalUserId.substring(0, Math.min(8, externalUserId.length()));
        }
        return "外部用户";
    }
}

