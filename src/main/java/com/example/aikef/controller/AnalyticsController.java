package com.example.aikef.controller;

import com.example.aikef.dto.analytics.AnalyticsSummaryDto;
import com.example.aikef.dto.analytics.AnalyticsTrendItemDto;
import com.example.aikef.model.enums.SubscriptionPlan;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.AnalyticsService;
import com.example.aikef.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/summary")
    public AnalyticsSummaryDto getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        SubscriptionPlan plan = subscriptionService.getPlan(tenantId);
        
        if (!plan.isSupportAnalyticsHistory()) {
            // Force to last 24 hours or today if plan doesn't support history
            Instant now = Instant.now();
            start = now.truncatedTo(ChronoUnit.DAYS);
            end = now;
        }
        
        return analyticsService.getSummary(tenantId, start, end);
    }

    @GetMapping("/trend")
    public List<AnalyticsTrendItemDto> getTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            Authentication authentication) {
        
        String tenantId = getTenantId(authentication);
        SubscriptionPlan plan = subscriptionService.getPlan(tenantId);

        if (!plan.isSupportAnalyticsHistory()) {
            // Force to last 24 hours or today if plan doesn't support history
            Instant now = Instant.now();
            start = now.truncatedTo(ChronoUnit.DAYS);
            end = now;
        }
        
        return analyticsService.getTrend(tenantId, start, end);
    }

    private String getTenantId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getTenantId();
        }
        throw new IllegalStateException("Authentication required (Agent)");
    }
}
