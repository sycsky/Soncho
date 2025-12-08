package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AiSuggestTagsRequest(@NotBlank String sessionId) {
}
