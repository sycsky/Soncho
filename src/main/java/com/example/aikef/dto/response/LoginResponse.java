package com.example.aikef.dto.response;

import com.example.aikef.dto.AgentDto;

public record LoginResponse(String token, AgentDto agent) {
}
