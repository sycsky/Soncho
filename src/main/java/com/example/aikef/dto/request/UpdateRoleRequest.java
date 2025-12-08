package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record UpdateRoleRequest(
        @NotBlank String name,
        String description,
        Map<String, Object> permissions) {
}
