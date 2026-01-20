package com.example.aikef.shopify.controller;

import com.example.aikef.shopify.dto.CreateSubscriptionRequest;
import com.example.aikef.shopify.dto.CreateSubscriptionResponse;
import com.example.aikef.shopify.service.ShopifyBillingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import com.example.aikef.shopify.dto.SubscriptionStatusDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shopify/billing")
@RequiredArgsConstructor
public class ShopifyBillingController {
    // Controller for Shopify Billing

    private final ShopifyBillingService billingService;

    @GetMapping("/current")
    public ResponseEntity<SubscriptionStatusDto> getCurrentSubscription(@RequestParam String shop) {
        return ResponseEntity.ok(billingService.getCurrentSubscription(shop));
    }

    @PostMapping("/subscription")
    public ResponseEntity<CreateSubscriptionResponse> createSubscription(
            @RequestParam String shop,
            @RequestBody CreateSubscriptionRequest request) throws JsonProcessingException {
        
        String confirmationUrl = billingService.createSubscription(shop, request.planId(), request.returnUrl());
        return ResponseEntity.ok(new CreateSubscriptionResponse(confirmationUrl));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifySubscription(
            @RequestParam String shop,
            @RequestBody Map<String, String> payload) {
        
        String chargeId = payload.get("chargeId");
        boolean success = billingService.verifySubscription(shop, chargeId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(@RequestParam String shop) {
        billingService.cancelSubscription(shop);
        return ResponseEntity.ok().build();
    }
}
