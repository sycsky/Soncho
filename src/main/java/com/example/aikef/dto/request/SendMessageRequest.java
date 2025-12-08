package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SendMessageRequest(
        @NotBlank String sessionId,
        @NotBlank String text,
        boolean isInternal,
        List<AttachmentPayload> attachments,
        List<String> mentions) {
}
