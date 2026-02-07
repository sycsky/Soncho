package com.example.aikef.shopify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ShopifyWebhookRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookRegistrationService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${shopify.app-url:http://localhost:8080}")
    private String appUrl;

    @Value("${shopify.api-version:2025-01}")
    private String apiVersion;

    public ShopifyWebhookRegistrationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    public void registerAll(String shopDomain, String accessToken) {
        Map<String, String> desired = desiredWebhooks();
        Map<String, ExistingWebhook> existing = listWebhooks(shopDomain, accessToken);

        for (Map.Entry<String, String> e : desired.entrySet()) {
            String topic = e.getKey();
            String address = e.getValue();
            ExistingWebhook ex = existing.get(topic);
            if (ex == null) {
                createWebhook(shopDomain, accessToken, topic, address);
                continue;
            }
            if (!address.equals(ex.address())) {
                updateWebhook(shopDomain, accessToken, ex.id(), address);
            }
        }
    }

    private Map<String, String> desiredWebhooks() {
        String webhooksBase = appUrl + "/api/v1/shopify/webhooks";
        Map<String, String> m = new LinkedHashMap<>();
        m.put("orders/create", webhooksBase + "/orders/create");
        m.put("orders/updated", webhooksBase + "/orders/updated");
        m.put("orders/paid", webhooksBase + "/orders/paid");
        m.put("orders/cancelled", webhooksBase + "/orders/cancelled");
        m.put("customers/create", webhooksBase + "/customers/create");
        m.put("customers/update", webhooksBase + "/customers/update");
        m.put("products/create", webhooksBase + "/products/create");
        m.put("products/update", webhooksBase + "/products/update");
        m.put("inventory_levels/update", webhooksBase + "/inventory_levels/update");
        m.put("refunds/create", webhooksBase + "/refunds/create");
        m.put("fulfillments/create", webhooksBase + "/fulfillments/create");
        m.put("fulfillments/update", webhooksBase + "/fulfillments/update");
        m.put("checkouts/create", webhooksBase + "/checkouts/create");
        m.put("checkouts/update", webhooksBase + "/checkouts/update");
        m.put("checkouts/delete", webhooksBase + "/checkouts/delete");
        m.put("draft_orders/create", webhooksBase + "/draft_orders/create");
        m.put("draft_orders/update", webhooksBase + "/draft_orders/update");
        m.put("collections/create", webhooksBase + "/collections/create");
        m.put("collections/update", webhooksBase + "/collections/update");
        m.put("themes/publish", webhooksBase + "/themes/publish");
        m.put("fulfillment_events/create", webhooksBase + "/fulfillment_events/create");
        m.put("app_subscriptions/update", webhooksBase + "/app_subscriptions/update");
        m.put("app/uninstalled", webhooksBase + "/app/uninstalled");
        return m;
    }

    private Map<String, ExistingWebhook> listWebhooks(String shopDomain, String accessToken) {
        String url = "https://" + shopDomain + "/admin/api/" + apiVersion + "/webhooks.json?limit=250";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String body = resp.getBody();
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode webhooks = root.get("webhooks");
            if (webhooks == null || !webhooks.isArray()) {
                return Map.of();
            }
            Map<String, ExistingWebhook> byTopic = new LinkedHashMap<>();
            Set<String> seenTopics = new LinkedHashSet<>();
            for (JsonNode wh : webhooks) {
                JsonNode topicNode = wh.get("topic");
                JsonNode addressNode = wh.get("address");
                JsonNode idNode = wh.get("id");
                if (topicNode == null || addressNode == null || idNode == null) {
                    continue;
                }
                String topic = topicNode.asText();
                if (topic == null || topic.isBlank()) {
                    continue;
                }
                if (seenTopics.contains(topic)) {
                    continue;
                }
                seenTopics.add(topic);
                byTopic.put(topic, new ExistingWebhook(idNode.asText(), addressNode.asText()));
            }
            return byTopic;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void createWebhook(String shopDomain, String accessToken, String topic, String address) {
        String url = "https://" + shopDomain + "/admin/api/" + apiVersion + "/webhooks.json";
        Map<String, Object> payload = Map.of(
                "webhook", Map.of(
                        "topic", topic,
                        "address", address,
                        "format", "json"
                )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            log.info("Registered webhook {} for shop {}", topic, shopDomain);
        } catch (Exception e) {
            log.error("Failed to register webhook {} for shop {}: {}", topic, shopDomain, e.getMessage());
        }
    }

    private void updateWebhook(String shopDomain, String accessToken, String webhookId, String address) {
        String url = "https://" + shopDomain + "/admin/api/" + apiVersion + "/webhooks/" + webhookId + ".json";
        Map<String, Object> payload = Map.of(
                "webhook", Map.of(
                        "address", address
                )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.put(url, new HttpEntity<>(payload, headers));
            log.info("Updated webhook {} for shop {}", webhookId, shopDomain);
        } catch (Exception e) {
            log.error("Failed to update webhook {} for shop {}: {}", webhookId, shopDomain, e.getMessage());
        }
    }

    private record ExistingWebhook(String id, String address) {}
}
