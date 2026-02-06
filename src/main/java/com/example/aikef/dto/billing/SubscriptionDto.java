package com.example.aikef.dto.billing;

import com.example.aikef.model.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SubscriptionDto {
    private SubscriptionPlan plan;
    private String status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private long aiUsage;
    private long seatUsage;
    private boolean cancelAtPeriodEnd;
    
    // Next Cycle Info
    private SubscriptionPlan nextPlan;
    private Instant nextBillingDate;
    private Double nextPrice;

    // Feature Flags
    private boolean supportAnalyticsHistory;
    private boolean supportAdvancedAnalytics;
    private boolean supportMagicRewrite;
    private boolean supportSmartSummary;
    private boolean supportAiTags;
}
