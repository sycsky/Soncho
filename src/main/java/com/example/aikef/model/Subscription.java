package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.model.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, CANCELED, PAST_DUE

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "shopify_charge_id")
    private String shopifyChargeId;

    @Column(name = "seat_usage")
    private Integer seatUsage = 0;

    @Column(name = "ai_usage")
    private Integer aiUsage = 0;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = false;

    public int getSeatUsage() {
        return seatUsage == null ? 0 : seatUsage;
    }

    public int getAiUsage() {
        return aiUsage == null ? 0 : aiUsage;
    }

    public boolean isCancelAtPeriodEnd() {
        return Boolean.TRUE.equals(cancelAtPeriodEnd);
    }
}
