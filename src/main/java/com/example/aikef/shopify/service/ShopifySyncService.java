package com.example.aikef.shopify.service;

import com.example.aikef.shopify.model.ShopifyObject;
import com.example.aikef.shopify.repository.ShopifyObjectRepository;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopifySyncService {

    private static final String TOKEN_KEY_PREFIX = "shopify:access_token:";

    private final ShopifyObjectRepository objectRepository;
    private final ShopifyStoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ShopifySyncService(ShopifyObjectRepository objectRepository,
                              ShopifyStoreRepository storeRepository,
                              ObjectMapper objectMapper,
                              StringRedisTemplate redisTemplate) {
        this.objectRepository = objectRepository;
        this.storeRepository = storeRepository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void upsertFromWebhook(String topic, String shopDomain, String webhookId, byte[] body) {
        if ("app/uninstalled".equals(topic)) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + shopDomain);
            storeRepository.findByShopDomain(shopDomain).ifPresent(store -> {
                store.setActive(false);
                store.setUninstalledAt(Instant.now());
                store.setAccessToken("");
                store.setScopes("");
                storeRepository.save(store);
            });
            return;
        }
        ShopifyObject.ObjectType objectType = mapTopicToObjectType(topic);
        if (objectType == null) {
            storeRepository.findByShopDomain(shopDomain).ifPresent(store -> {
                store.setLastSyncedAt(Instant.now());
                storeRepository.save(store);
            });
            return;
        }

        String payloadJson = new String(body, StandardCharsets.UTF_8);
        String externalId = extractId(payloadJson);
        if (externalId == null || externalId.isBlank()) {
            return;
        }

        ShopifyObject obj = objectRepository
                .findByShopDomainAndObjectTypeAndExternalId(shopDomain, objectType, externalId)
                .orElseGet(ShopifyObject::new);

        obj.setShopDomain(shopDomain);
        obj.setObjectType(objectType);
        obj.setExternalId(externalId);
        obj.setPayloadJson(payloadJson);
        obj.setLastSeenAt(Instant.now());
        obj.setSourceWebhookId(webhookId);
        objectRepository.save(obj);

        storeRepository.findByShopDomain(shopDomain).ifPresent(store -> {
            store.setLastSyncedAt(Instant.now());
            storeRepository.save(store);
        });
    }

    private ShopifyObject.ObjectType mapTopicToObjectType(String topic) {
        if (topic == null) {
            return null;
        }
        return switch (topic) {
            case "orders/create", "orders/updated" -> ShopifyObject.ObjectType.ORDER;
            case "customers/create", "customers/update" -> ShopifyObject.ObjectType.CUSTOMER;
            case "products/create", "products/update" -> ShopifyObject.ObjectType.PRODUCT;
            default -> null;
        };
    }

    private String extractId(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode idNode = root.get("id");
            if (idNode == null || idNode.isNull()) {
                return null;
            }
            return idNode.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
