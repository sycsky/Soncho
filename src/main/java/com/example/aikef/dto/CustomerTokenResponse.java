package com.example.aikef.dto;

import java.util.UUID;

public record CustomerTokenResponse(
        UUID customerId,
        String token,
        String name,
        String channel,
        UUID sessionId
) {
}
