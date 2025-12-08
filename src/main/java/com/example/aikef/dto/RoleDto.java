package com.example.aikef.dto;

import java.util.Map;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        String description,
        boolean system,
        Map<String, Object> permissions) {
}
