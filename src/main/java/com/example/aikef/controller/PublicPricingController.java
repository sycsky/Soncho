package com.example.aikef.controller;

import com.example.aikef.model.enums.SubscriptionPlan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/plans")
public class PublicPricingController {

    @GetMapping
    public List<Map<String, Object>> getPlans() {
        return Arrays.stream(SubscriptionPlan.values())
                .map(plan -> Map.<String, Object>of(
                        "name", plan.name(),
                        "displayName", plan.getDisplayName(),
                        "price", plan.getPrice(),
                        "aiLimit", plan.getAiLimit(),
                        "seatLimit", plan.getSeatLimit(),
                        "supportAnalyticsHistory", plan.isSupportAnalyticsHistory(),
                        "supportAdvancedAnalytics", plan.isSupportAdvancedAnalytics(),
                        "supportMagicRewrite", plan.isSupportMagicRewrite(),
                        "supportSmartSummary", plan.isSupportSmartSummary(),
                        "supportAiTags", plan.isSupportAiTags()
                ))
                .collect(Collectors.toList());
    }
}
