package com.example.aikef.dto;

import java.util.UUID;

public record QuickReplyDto(
        UUID id,
        String label,
        String text,
        String category,
        boolean system) {
}
