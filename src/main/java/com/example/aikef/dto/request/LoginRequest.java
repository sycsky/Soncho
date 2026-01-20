package com.example.aikef.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record LoginRequest(
        @Email String email,
        UUID agentId,
        @NotBlank String password,
        String shopifySessionToken,
        String shop) {
}
