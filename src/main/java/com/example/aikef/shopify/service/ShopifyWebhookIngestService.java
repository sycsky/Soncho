package com.example.aikef.shopify.service;

import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.shopify.model.ShopifyWebhookEvent;
import com.example.aikef.shopify.repository.ShopifyWebhookEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopifyWebhookIngestService {

    private final ShopifyWebhookEventRepository eventRepository;
    private final ShopifySyncService syncService;

    public ShopifyWebhookIngestService(ShopifyWebhookEventRepository eventRepository, ShopifySyncService syncService) {
        this.eventRepository = eventRepository;
        this.syncService = syncService;
    }

    @Transactional
    public void ingest(String topic, String shopDomain, String apiVersion, String webhookId, Instant triggeredAt, byte[] body) {
        if (shopDomain == null || shopDomain.isBlank()) {
            throw new IllegalArgumentException("Missing X-Shopify-Shop-Domain");
        }
        if (webhookId == null || webhookId.isBlank()) {
            throw new IllegalArgumentException("Missing X-Shopify-Webhook-Id");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Missing topic");
        }

        String tenantId = ShopifyAuthService.generateTenantId(shopDomain);
        TenantContext.setTenantId(tenantId);
        try {
            Optional<ShopifyWebhookEvent> existing = eventRepository.findByShopDomainAndWebhookId(shopDomain, webhookId);
            ShopifyWebhookEvent event = existing.orElseGet(ShopifyWebhookEvent::new);

            event.setWebhookId(webhookId);
            event.setShopDomain(shopDomain);
            event.setTopic(topic);
            event.setApiVersion(apiVersion);
            event.setTriggeredAt(triggeredAt);
            event.setPayloadJson(new String(body, StandardCharsets.UTF_8));

            if (existing.isEmpty()) {
                eventRepository.save(event);
            }

            try {
                syncService.upsertFromWebhook(topic, shopDomain, webhookId, body);
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                event.setProcessingError(null);
            } catch (Exception e) {
                event.setProcessed(false);
                event.setProcessedAt(null);
                String msg = e.getMessage();
                event.setProcessingError(msg == null ? e.getClass().getSimpleName() : msg.substring(0, Math.min(2000, msg.length())));
            }

            eventRepository.save(event);
        } finally {
            TenantContext.clear();
        }
    }
}
