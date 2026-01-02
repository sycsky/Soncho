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
@RequestMapping("/api/v1/shopify/gdpr")
public class ShopifyGdprController {

    private final ShopifyWebhookVerifier verifier;
    private final ShopifyWebhookIngestService ingestService;

    public ShopifyGdprController(ShopifyWebhookVerifier verifier, ShopifyWebhookIngestService ingestService) {
        this.verifier = verifier;
        this.ingestService = ingestService;
    }

    @PostMapping("/customers/data_request")
    public ResponseEntity<Map<String, Object>> customersDataRequest(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("gdpr/customers/data_request", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/customers/redact")
    public ResponseEntity<Map<String, Object>> customersRedact(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("gdpr/customers/redact", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
    }

    @PostMapping("/shop/redact")
    public ResponseEntity<Map<String, Object>> shopRedact(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Shopify-API-Version", required = false) String apiVersion,
            @RequestHeader(value = "X-Shopify-Triggered-At", required = false) String triggeredAt,
            @RequestBody byte[] body
    ) {
        return verifyIngestAndOk("gdpr/shop/redact", hmac, shopDomain, webhookId, apiVersion, triggeredAt, body);
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
