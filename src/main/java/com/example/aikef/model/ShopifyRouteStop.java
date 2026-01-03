package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "shopify_route_stops")
@Data
public class ShopifyRouteStop extends AuditableEntity {

    @ManyToOne
    @JoinColumn(name = "route_id", nullable = false)
    private ShopifyRoute route;

    @Column(name = "shopify_order_id")
    private String shopifyOrderId;

    private Integer sequence; // Order in the route

    private String location; // Address or Coordinates

    private String status; // PENDING, COMPLETED, SKIPPED

    @Column(columnDefinition = "TEXT")
    private String customerInfo; // Snapshot of customer info for delivery
}
