package com.example.aikef.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AnalyticsSummaryDto {
    private long totalConversations;
    private long aiHandledCount;
    private long humanHandledCount;
    private long totalMessages;
    private long aiMessages;
    private long humanMessages;
    private Map<String, Long> orderActions;
    private Map<String, Long> topTags;
    private Map<String, Long> sessionStatusDistribution;
}
