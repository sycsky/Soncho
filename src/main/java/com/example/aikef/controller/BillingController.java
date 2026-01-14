package com.example.aikef.controller;

import com.example.aikef.dto.billing.SubscriptionDto;
import com.example.aikef.model.enums.SubscriptionPlan;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public SubscriptionDto getCurrentSubscription(Authentication authentication) {
        String tenantId = getTenantId(authentication);
        return subscriptionService.getCurrentSubscription(tenantId);
    }

    @PostMapping("/change-plan")
    public void changePlan(@RequestBody Map<String, String> request, Authentication authentication) {
        String tenantId = getTenantId(authentication);
        String planName = request.get("plan");
        SubscriptionPlan plan = SubscriptionPlan.valueOf(planName.toUpperCase());
        subscriptionService.changePlan(tenantId, plan);
    }

    private String getTenantId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getTenantId();
        }
        throw new IllegalStateException("Authentication required (Agent)");
    }
}
