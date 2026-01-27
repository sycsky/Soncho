package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Old password is required")
        String oldPassword,
        
        @NotBlank(message = "New password is required")
        String newPassword
) {}
