package com.example.aikef.dto.request;

public record AiRewriteRequest(
    String text, 
    
    String tone,
    
    String sessionId,
    String language
) {}
