package com.example.aikef.shopify.service;

import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.shopify.model.ShopifyWebhookEvent;
import com.example.aikef.shopify.repository.ShopifyWebhookEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopifyWebhookIngestService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookIngestService.class);
    private final ShopifyWebhookEventRepository eventRepository;
    private final ShopifySyncService syncService;
    private final RedissonClient redissonClient;

    public ShopifyWebhookIngestService(ShopifyWebhookEventRepository eventRepository, 
                                     ShopifySyncService syncService,
                                     RedissonClient redissonClient) {
        this.eventRepository = eventRepository;
        this.syncService = syncService;
        this.redissonClient = redissonClient;
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
        
        // 1. Acquire Distributed Lock (Redisson) to handle concurrent identical webhooks
        String lockKey = "lock:shopify:webhook:" + webhookId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Try to acquire lock, wait up to 0s (don't wait), lease time 10m
            // If we can't get it immediately, it means another thread is already processing it
            if (!lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                log.warn("Webhook {} is already being processed (lock exists), skipping concurrent request.", webhookId);
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for webhook {}", webhookId, e);
            return;
        }

        try {
            // 2. Check Database for idempotency
            Optional<ShopifyWebhookEvent> existing = eventRepository.findByShopDomainAndWebhookId(shopDomain, webhookId);
            
            if (existing.isPresent() && existing.get().isProcessed()) {
                log.info("Webhook {} already processed for shop {}, skipping.", webhookId, shopDomain);
                return;
            }

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
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Concurrent insert detected for Webhook {} (DB Constraint), skipping.", webhookId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                // We keep the lock until the transaction is committed or rolled back.
                // However, @Transactional commits AFTER this finally block.
                // To be safe, we could unlock here, but the DB unique constraint and isProcessed check 
                // will handle the race condition if another thread acquires the lock before this one commits.
                // But Redisson lock is more about preventing multiple threads from doing the same expensive work simultaneously.
                lock.unlock();
            }
            TenantContext.clear();
        }
    }
}
