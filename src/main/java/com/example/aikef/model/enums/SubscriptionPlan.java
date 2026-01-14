package com.example.aikef.model.enums;

import lombok.Getter;

@Getter
public enum SubscriptionPlan {
    // Free: Today's data only, Basic Q&A
    FREE("Free", 0, 50, 1, false, false, false, false, false),
    
    // Basic: History (7/30d), Magic Rewrite
    BASIC("Basic", 19, 500, 3, true, false, true, false, false),
    
    // Pro: Advanced Analytics, Smart Summary, AI Tags
    PRO("Pro", 59, 2000, 10, true, true, true, true, true),
    
    // Enterprise: Full Export, Custom Models
    ENTERPRISE("Enterprise", 199, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, true, true, true);

    private final String displayName;
    private final int price;
    private final int aiLimit;
    private final int seatLimit;
    
    // Feature Flags
    private final boolean supportAnalyticsHistory; // Can view history > 1 day
    private final boolean supportAdvancedAnalytics; // Sentiment, AI Insights
    private final boolean supportMagicRewrite;
    private final boolean supportSmartSummary;
    private final boolean supportAiTags;

    SubscriptionPlan(String displayName, int price, int aiLimit, int seatLimit,
                     boolean supportAnalyticsHistory, boolean supportAdvancedAnalytics,
                     boolean supportMagicRewrite, boolean supportSmartSummary, boolean supportAiTags) {
        this.displayName = displayName;
        this.price = price;
        this.aiLimit = aiLimit;
        this.seatLimit = seatLimit;
        this.supportAnalyticsHistory = supportAnalyticsHistory;
        this.supportAdvancedAnalytics = supportAdvancedAnalytics;
        this.supportMagicRewrite = supportMagicRewrite;
        this.supportSmartSummary = supportSmartSummary;
        this.supportAiTags = supportAiTags;
    }
}
