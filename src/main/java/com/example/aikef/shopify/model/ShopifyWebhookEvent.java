package com.example.aikef.shopify.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "shopify_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "webhook_id"}, name = "uk_shopify_webhook_tenant_webhook_id")
        },
        indexes = {
                @Index(columnList = "tenant_id, shop_domain, topic", name = "idx_shopify_webhook_tenant_shop_topic"),
                @Index(columnList = "tenant_id, processed", name = "idx_shopify_webhook_tenant_processed")
        }
)
@Getter
@Setter
public class ShopifyWebhookEvent extends AuditableEntity {

    @Column(name = "webhook_id", nullable = false, length = 120)
    private String webhookId;

    @Column(name = "shop_domain", nullable = false, length = 255)
    private String shopDomain;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "api_version", length = 40)
    private String apiVersion;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_error", length = 2000)
    private String processingError;
}

