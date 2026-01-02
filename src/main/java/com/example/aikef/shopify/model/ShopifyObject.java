package com.example.aikef.shopify.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "shopify_objects",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "shop_domain", "object_type", "external_id"}, name = "uk_shopify_object_tenant_shop_type_external")
        },
        indexes = {
                @Index(columnList = "tenant_id, shop_domain, object_type", name = "idx_shopify_object_tenant_shop_type"),
                @Index(columnList = "tenant_id, external_id", name = "idx_shopify_object_tenant_external")
        }
)
@Getter
@Setter
public class ShopifyObject extends AuditableEntity {

    public enum ObjectType {
        ORDER,
        CUSTOMER,
        PRODUCT
    }

    @Column(name = "shop_domain", nullable = false, length = 255)
    private String shopDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 20)
    private ObjectType objectType;

    @Column(name = "external_id", nullable = false, length = 60)
    private String externalId;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "source_webhook_id", length = 120)
    private String sourceWebhookId;
}

