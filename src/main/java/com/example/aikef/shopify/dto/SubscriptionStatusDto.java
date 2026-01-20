package com.example.aikef.shopify.dto;

import java.time.Instant;

public record SubscriptionStatusDto(
    boolean active,
    String planId,
    int trialDaysRemaining,
    Instant currentPeriodEnd,
    String status,
    boolean cancelAtPeriodEnd
) {}
