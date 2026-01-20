package com.example.aikef.shopify.dto;

public record CreateSubscriptionRequest(
    String planId,
    String returnUrl
) {}
