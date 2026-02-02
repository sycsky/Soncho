package com.example.aikef.shopify.service;

import com.example.aikef.shopify.model.ShopifyObject;
import com.example.aikef.shopify.repository.ShopifyObjectRepository;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import com.example.aikef.service.EventService;
import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ShopifySyncService {

    private static final Logger log = LoggerFactory.getLogger(ShopifySyncService.class);
    private static final String TOKEN_KEY_PREFIX = "shopify:access_token:";

    private static final Set<String> CUSTOMER_RELATED_TOPICS = new HashSet<>(Arrays.asList(
            "orders/create", "orders/updated", "orders/cancelled",
            "customers/create", "customers/update",
            "checkouts/create", "checkouts/update", "checkouts/delete",
            "draft_orders/create", "draft_orders/update",
            "refunds/create",
            "fulfillments/create", "fulfillments/update",
            "fulfillment_events/create"
    ));

    private final ShopifyObjectRepository objectRepository;
    private final ShopifyStoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final ShopifyGdprService gdprService;
    private final EventService eventService;
    private final CustomerRepository customerRepository;
    private ShopifySyncService self;

    public ShopifySyncService(ShopifyObjectRepository objectRepository,
                              ShopifyStoreRepository storeRepository,
                              ObjectMapper objectMapper,
                              StringRedisTemplate redisTemplate,
                              ShopifyGdprService gdprService,
                              @Lazy EventService eventService,
                              CustomerRepository customerRepository) {
        this.objectRepository = objectRepository;
        this.storeRepository = storeRepository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.gdprService = gdprService;
        this.eventService = eventService;
        this.customerRepository = customerRepository;
    }

    @Autowired
    public void setSelf(@Lazy ShopifySyncService self) {
        this.self = self;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void updateStoreSyncTime(String shopDomain) {
        String cacheKey = "shopify:store_sync_throttle:" + shopDomain;
        String lastUpdate = redisTemplate.opsForValue().get(cacheKey);
        if (lastUpdate != null) {
            // Throttle: only update DB once every 60 seconds
            return;
        }

        try {
            storeRepository.updateLastSyncedAt(shopDomain, Instant.now());
            redisTemplate.opsForValue().set(cacheKey, "1", Duration.ofSeconds(60));
        } catch (Exception e) {
            log.warn("Failed to update store sync time for {}: {}", shopDomain, e.getMessage());
        }
    }

    @Transactional
    public void upsertFromWebhook(String topic, String shopDomain, String webhookId, byte[] body) {
        // GDPR Webhooks
        if (topic != null && topic.startsWith("gdpr/")) {
            String payloadJson = new String(body, StandardCharsets.UTF_8);
            switch (topic) {
                case "gdpr/customers/data_request" -> gdprService.handleCustomerDataRequest(shopDomain, payloadJson);
                case "gdpr/customers/redact" -> gdprService.handleCustomerRedact(shopDomain, payloadJson);
                case "gdpr/shop/redact" -> gdprService.handleShopRedact(shopDomain);
                default -> { /* ignore unknown gdpr topics */ }
            }
            return;
        }

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
            self.updateStoreSyncTime(shopDomain);
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

        boolean isNew;
        if (objectType == ShopifyObject.ObjectType.CHECKOUT) {
             String redisKey = "shopify:checkout:processed:" + shopDomain + ":" + externalId;
             Boolean hasKey = redisTemplate.hasKey(redisKey);
             if (Boolean.TRUE.equals(hasKey)) {
                 isNew = false;
             } else {
                 isNew = true;
                 redisTemplate.opsForValue().set(redisKey, "1", Duration.ofDays(10));
             }
        } else {
             isNew = obj.getId() == null;
        }

        obj.setShopDomain(shopDomain);
        obj.setObjectType(objectType);
        obj.setExternalId(externalId);
        obj.setPayloadJson(payloadJson);
        obj.setLastSeenAt(Instant.now());
        obj.setSourceWebhookId(webhookId);
        objectRepository.save(obj);

        self.updateStoreSyncTime(shopDomain);

        if ("checkouts/create".equals(topic) && !isNew) {
            log.info("Duplicate checkouts/create event for checkout {}, skipping event trigger.", externalId);
            return;
        }

        log.info("payloadJson:{}",payloadJson);
        // Trigger Event
        try {
            String eventName = "shopify." + topic.replace("/", ".");
            Map<String, Object> eventData = new HashMap<>();
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = objectMapper.readValue(payloadJson, Map.class);
                if (payloadMap != null) {
                    eventData.putAll(payloadMap);

                    boolean isPaid = false;
                    Object financialStatus = payloadMap.get("financial_status");
                    if ("paid".equals(financialStatus)) {
                        isPaid = true;
                    }
                    eventData.put("isPaid", isPaid);
                }
            } catch (Exception e) {
                // ignore
            }
            
            eventData.put("topic", topic);
            eventData.put("shopDomain", shopDomain);
            eventData.put("webhookId", webhookId);
            
            if (CUSTOMER_RELATED_TOPICS.contains(topic)) {
                Customer customer = findAssociatedCustomer(topic, shopDomain, payloadJson);
                if (customer != null) {
                    eventService.triggerEventForCustomerAsync(customer.getId(), eventName, eventData);
                } else {
                    log.warn("Skipping customer-related event {} because no associated customer found for shop {}", eventName, shopDomain);
                    // Still trigger a generic event so it's visible in the system logs/traces
//                    eventService.triggerEvent(eventName, null, eventData);
                }
            } else {
                // Non-customer related events: trigger with generic method (mock session)
                String tenantId = storeRepository.findByShopDomain(shopDomain)
                        .map(store -> store.getTenantId())
                        .orElse(null);
                eventService.triggerEventAsync(eventName, null, eventData, tenantId);
            }
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to trigger event: " + e.getMessage());
        }
    }

    private Customer findAssociatedCustomer(String topic, String shopDomain, String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            String shopifyCustomerId = null;
            String email = null;

            if (topic.startsWith("customers/")) {
                if (root.has("id")) shopifyCustomerId = root.get("id").asText();
                if (root.has("email")) email = root.get("email").asText();
            } else if (topic.startsWith("orders/") || topic.startsWith("draft_orders/") || topic.startsWith("checkouts/")) {
                JsonNode customerNode = root.path("customer");
                if (!customerNode.isMissingNode()) {
                    if (customerNode.has("id")) shopifyCustomerId = customerNode.get("id").asText();
                    if (customerNode.has("email")) email = customerNode.get("email").asText();
                }
                // Check root email if not found in customer object (common in checkouts)
                if (email == null && root.has("email")) {
                    email = root.get("email").asText();
                }
            } else if (topic.startsWith("refunds/") || topic.startsWith("fulfillments/") || topic.startsWith("fulfillment_events/")) {
                // Try to find via order_id
                String orderId = root.path("order_id").asText(null);
                if (orderId != null) {
                    // Look up the order in our DB
                     Optional<ShopifyObject> orderOpt = objectRepository.findByShopDomainAndObjectTypeAndExternalId(
                            shopDomain, ShopifyObject.ObjectType.ORDER, orderId);
                     if (orderOpt.isPresent()) {
                         try {
                             JsonNode orderRoot = objectMapper.readTree(orderOpt.get().getPayloadJson());
                             JsonNode customerNode = orderRoot.path("customer");
                             if (!customerNode.isMissingNode()) {
                                 if (customerNode.has("id")) shopifyCustomerId = customerNode.get("id").asText();
                                 if (customerNode.has("email")) email = customerNode.get("email").asText();
                             }
                         } catch (Exception e) {
                             // ignore parsing error
                         }
                     }
                }
            }

            if (shopifyCustomerId != null && !shopifyCustomerId.isBlank()) {
                Optional<Customer> c = customerRepository.findByShopifyCustomerId(shopifyCustomerId);
                if (c.isPresent()) return c.get();
            }
            if (email != null && !email.isBlank()) {
                Optional<Customer> c = customerRepository.findByEmail(email);
                if (c.isPresent()) return c.get();
            }
        } catch (Exception e) {
            System.err.println("Error finding associated customer: " + e.getMessage());
        }
        return null;
    }

    private ShopifyObject.ObjectType mapTopicToObjectType(String topic) {
        if (topic == null) {
            return null;
        }
        return switch (topic) {
            case "orders/create", "orders/updated", "orders/cancelled", "orders/paid" -> ShopifyObject.ObjectType.ORDER;
            case "customers/create", "customers/update" -> ShopifyObject.ObjectType.CUSTOMER;
            case "products/create", "products/update" -> ShopifyObject.ObjectType.PRODUCT;
            case "inventory_levels/update" -> ShopifyObject.ObjectType.INVENTORY_LEVEL;
            case "refunds/create" -> ShopifyObject.ObjectType.REFUND;
            case "fulfillments/create", "fulfillments/update" -> ShopifyObject.ObjectType.FULFILLMENT;
            case "checkouts/create", "checkouts/update", "checkouts/delete" -> ShopifyObject.ObjectType.CHECKOUT;
            case "draft_orders/create", "draft_orders/update" -> ShopifyObject.ObjectType.DRAFT_ORDER;
            case "collections/create", "collections/update" -> ShopifyObject.ObjectType.COLLECTION;
            case "themes/publish" -> ShopifyObject.ObjectType.THEME;
            case "fulfillment_events/create" -> ShopifyObject.ObjectType.FULFILLMENT_EVENT;
            default -> null;
        };
    }

    private String extractId(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode idNode = root.get("id");
            if (idNode != null && !idNode.isNull()) {
                return idNode.asText();
            }
            // Fallback for inventory levels
            JsonNode itemId = root.get("inventory_item_id");
            JsonNode locId = root.get("location_id");
            if (itemId != null && locId != null) {
                return itemId.asText() + "_" + locId.asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
