package com.example.aikef.dto;

import com.example.aikef.model.OrderCancellationPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCancellationPolicyDto(
        UUID id,
        String name,
        String description,
        Integer cancellableHours,
        BigDecimal penaltyPercentage,
        boolean enabled,
        boolean isDefault,
        Integer sortOrder,
        OrderCancellationPolicy.PolicyType policyType,
        Instant createdAt,
        Instant updatedAt
) {
}

