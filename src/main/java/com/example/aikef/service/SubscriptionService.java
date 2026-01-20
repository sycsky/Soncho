package com.example.aikef.service;

import com.example.aikef.dto.billing.SubscriptionDto;
import com.example.aikef.model.Subscription;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SubscriptionPlan;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MessageRepository messageRepository;
    private final AgentRepository agentRepository;

    @Transactional
    public SubscriptionDto getCurrentSubscription(String tenantId) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        // Check if expired
        if (sub.getCurrentPeriodEnd() != null && sub.getCurrentPeriodEnd().isBefore(Instant.now())) {
            // Only auto-renew FREE plan
            if (sub.getPlan() == SubscriptionPlan.FREE) {
                renewSubscription(sub);
            }
            // For paid plans, we wait for Shopify webhook or scheduler to update/downgrade.
            // If it's expired here, it means it hasn't been renewed yet.
            // Ideally we should treat it as expired/inactive in feature checks.
        }

        long aiUsage = messageRepository.countByCreatedAtBetweenAndTenantIdAndSenderType(
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd() : Instant.now(),
                tenantId,
                SenderType.AI
        );

        long seatUsage = agentRepository.countByTenantId(tenantId);

        return SubscriptionDto.builder()
                .plan(sub.getPlan())
                .status(sub.getStatus())
                .currentPeriodStart(sub.getCurrentPeriodStart())
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .aiUsage(aiUsage)
                .seatUsage(seatUsage)
                .cancelAtPeriodEnd(sub.isCancelAtPeriodEnd())
                .supportAnalyticsHistory(sub.getPlan().isSupportAnalyticsHistory())
                .supportAdvancedAnalytics(sub.getPlan().isSupportAdvancedAnalytics())
                .supportMagicRewrite(sub.getPlan().isSupportMagicRewrite())
                .supportSmartSummary(sub.getPlan().isSupportSmartSummary())
                .supportAiTags(sub.getPlan().isSupportAiTags())
                .build();
    }

    @Transactional
    public void changePlan(String tenantId, SubscriptionPlan newPlan) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));
        
        sub.setPlan(newPlan);
        // In real world, we might reset period or keep it.
        // For simulation, we keep the period but update the plan.
        subscriptionRepository.save(sub);
    }

    private Subscription createDefaultSubscription(String tenantId) {
        Subscription sub = new Subscription();
        sub.setTenantId(tenantId);
        sub.setPlan(SubscriptionPlan.FREE);
        sub.setStatus("ACTIVE");
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        return subscriptionRepository.save(sub);
    }

    private void renewSubscription(Subscription sub) {
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        subscriptionRepository.save(sub);
    }
    
    public boolean checkAiLimit(String tenantId) {
        SubscriptionDto current = getCurrentSubscription(tenantId);
        if (current.getPlan() == SubscriptionPlan.ENTERPRISE) return true;
        return current.getAiUsage() < current.getPlan().getAiLimit();
    }
    
    public SubscriptionPlan getPlan(String tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .map(Subscription::getPlan)
                .orElse(SubscriptionPlan.FREE);
    }
}
