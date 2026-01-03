package com.example.aikef.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Data
public class PurchaseOrder {

    @Id
    @Column(length = 6, nullable = false)
    private String id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false)
    private Customer initiator;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Customer supplier;

    @Column(nullable = false)
    private String status; // ORDERED, SHIPPED, RECEIVED

    private BigDecimal totalAmount;
    private BigDecimal payableAmount;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = String.format("%06d", new java.util.Random().nextInt(1000000));
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
