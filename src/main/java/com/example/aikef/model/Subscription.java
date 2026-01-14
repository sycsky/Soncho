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
}
