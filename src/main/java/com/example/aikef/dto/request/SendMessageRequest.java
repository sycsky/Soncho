package com.example.aikef.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 发送消息请求
 * text 和 attachments 至少有一个不为空
 */
public record SendMessageRequest(
        @NotBlank String sessionId,
        String text,  // 如果有附件，text 可以为空
        boolean isInternal,
        List<AttachmentPayload> attachments,
        List<String> mentions) {
    
    /**
     * 验证：text 和 attachments 至少有一个不为空
     */
    @AssertTrue(message = "消息内容和附件不能同时为空")
    public boolean isContentValid() {
        boolean hasText = text != null && !text.isBlank();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        return hasText || hasAttachments;
    }
}
