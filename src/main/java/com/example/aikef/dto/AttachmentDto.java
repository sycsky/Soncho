package com.example.aikef.dto;

import com.example.aikef.model.enums.AttachmentType;
import java.util.UUID;

public record AttachmentDto(
        UUID id,
        AttachmentType type,
        String url,
        String name,
        Integer sizeKb) {
}
