package com.example.aikef.dto.request;

import com.example.aikef.model.enums.AttachmentType;
import jakarta.validation.constraints.NotBlank;

public record AttachmentPayload(
        AttachmentType type,
        @NotBlank String url,
        @NotBlank String name,
        Integer sizeKb) {
}
