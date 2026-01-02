package com.example.aikef.shopify.controller;

import com.example.aikef.shopify.service.ShopifyWebhookIngestService;
import com.example.aikef.shopify.service.ShopifyWebhookVerifier;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shopify/webhooks")
public class ShopifyWebhookController {

    private final ShopifyWebhookVerifier verifier;
    private final ShopifyWebhookIngestService ingestService;

    public ShopifyWebhookController(ShopifyWebhookVerifier verifier, ShopifyWebhookIngestService ingestService) {
        this.verifier = verifier;
        this.ingestService = ingestService;
    }

    @PostMapping("/orders/create")
    public ResponseEntity<Map<String, Object>> ordersCreate(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("orders/create", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/orders/updated")
    public ResponseEntity<Map<String, Object>> ordersUpdated(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("orders/updated", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/customers/create")
    public ResponseEntity<Map<String, Object>> customersCreate(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("customers/create", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/customers/update")
    public ResponseEntity<Map<String, Object>> customersUpdate(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("customers/update", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/products/create")
    public ResponseEntity<Map<String, Object>> productsCreate(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("products/create", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/products/update")
    public ResponseEntity<Map<String, Object>> productsUpdate(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("products/update", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/app/uninstalled")
    public ResponseEntity<Map<String, Object>> appUninstalled(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("app/uninstalled", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    private ResponseEntity<Map<String, Object>> verifyIngestAndOk(
            String topic,
            String hmac,
            String shopDomain,
            String webhookId,
            String apiVersion,
            String triggeredAt,
            byte[] body
    ) {
        if (!verifier.verify(hmac, body)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }
        try {
            ingestService.ingest(topic, shopDomain, apiVersion, webhookId, parseTriggeredAt(triggeredAt), body);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private Instant parseTriggeredAt(String triggeredAt) {
        if (triggeredAt == null || triggeredAt.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(triggeredAt);
        } catch (Exception e) {
            return null;
        }
    }
}
