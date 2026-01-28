package com.example.aikef.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @Email(message = "Invalid email format")
        String email,
        
        String avatarUrl,
        
        String name
) {}
