package com.example.aikef.dto.websocket;

import com.fasterxml.jackson.databind.JsonNode;

public record WebSocketEnvelope(String eventId ,Long timestamp ,String event, JsonNode payload) {
}
