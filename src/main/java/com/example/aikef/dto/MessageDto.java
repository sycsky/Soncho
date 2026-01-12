package com.example.aikef.dto;

import com.example.aikef.model.enums.MessageType;
import com.example.aikef.model.enums.SenderType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID sessionId,
        SenderType senderType,
        MessageType messageType,
        UUID agentId,
        String text,
        boolean internal,
        Map<String, Object> translationData,
        List<String> mentions,
        List<AttachmentDto> attachments,
        Instant createdAt) {
}
