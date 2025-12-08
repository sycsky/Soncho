package com.example.aikef.dto;

import com.example.aikef.model.Channel;
import java.time.Instant;

public record ChatResponse(Channel Channel,
                           String conversationId,
                           String recipientId,
                           String reply,
                           Instant timestamp) {
}
