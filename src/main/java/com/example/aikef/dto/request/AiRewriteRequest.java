package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AiRewriteRequest(
    @NotBlank(message = "Text is required") 
    String text, 
    
    String tone,
    
    String sessionId
) {}
