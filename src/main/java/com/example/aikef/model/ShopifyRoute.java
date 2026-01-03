package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shopify_routes")
@Data
public class ShopifyRoute extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Customer driver; // The Logistics personnel

    @Column(nullable = false)
    private String status; // PLANNED, IN_PROGRESS, COMPLETED

    private boolean optimized;
}
