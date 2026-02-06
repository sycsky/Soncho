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
            // Handle expiration/renewal logic
            handleSubscriptionRenewal(sub);
        }

        long aiUsage = messageRepository.countByCreatedAtBetweenAndTenantIdAndSenderType(
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd() : Instant.now(),
                tenantId,
                SenderType.AI
        );

        long seatUsage = agentRepository.countByTenantId(tenantId);

        SubscriptionDto.SubscriptionDtoBuilder builder = SubscriptionDto.builder()
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
                .supportAiTags(sub.getPlan().isSupportAiTags());

        // Populate next cycle info
        if (sub.getNextPlan() != null) {
            builder.nextPlan(sub.getNextPlan())
                   .nextBillingDate(sub.getCurrentPeriodEnd())
                   .nextPrice((double) sub.getNextPlan().getPrice());
        } else if (!sub.isCancelAtPeriodEnd()) {
            // Auto-renew to same plan
            builder.nextPlan(sub.getPlan())
                   .nextBillingDate(sub.getCurrentPeriodEnd())
                   .nextPrice((double) sub.getPlan().getPrice());
        }

        return builder.build();
    }

    private void handleSubscriptionRenewal(Subscription sub) {
        // If there is a pending plan change, apply it now
        if (sub.getNextPlan() != null) {
            sub.setPlan(sub.getNextPlan());
            sub.setNextPlan(null);
            renewSubscription(sub);
        } else if (sub.getPlan() == SubscriptionPlan.FREE) {
            // Only auto-renew FREE plan if no pending change
            renewSubscription(sub);
        }
        // For paid plans without pending change, we wait for payment/webhook
    }

    @Transactional
    public void changePlan(String tenantId, SubscriptionPlan newPlan) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));
        
        // Check for downgrade
        if (sub.getPlan() != null && newPlan.getPrice() < sub.getPlan().getPrice()) {
            // Downgrade: set next plan, effective next cycle
            sub.setNextPlan(newPlan);
        } else {
            // Upgrade or same: immediate effect
            sub.setPlan(newPlan);
            sub.setNextPlan(null); // Clear any pending downgrade
        }
        
        // Ensure we have a period set
        if (sub.getCurrentPeriodEnd() == null) {
            sub.setCurrentPeriodStart(Instant.now());
            sub.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        }
        
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
