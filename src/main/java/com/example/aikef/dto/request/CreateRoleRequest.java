package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record CreateRoleRequest(
        @NotBlank(message = "Role name cannot be blank")
        String name,
        String description,
        Set<String> permissions
) {}
