package com.example.aikef.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * 精简版消息 DTO，匹配 bootstrap 接口所需字段
 */
public record SessionMessageDto(
        UUID id,
        String text,
        String sender,
        String messageType,
        long timestamp,
        @JsonProperty("isInternal") boolean internal,
        List<AttachmentDto> attachments,
        List<String> mentions
) {
}
