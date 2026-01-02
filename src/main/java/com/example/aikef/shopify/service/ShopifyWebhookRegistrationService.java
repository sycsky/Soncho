package com.example.aikef.shopify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ShopifyWebhookRegistrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${shopify.app-url:http://localhost:8080}")
    private String appUrl;

    @Value("${shopify.api-version:2025-10}")
    private String apiVersion;

    public ShopifyWebhookRegistrationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

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
        String gdprBase = appUrl + "/api/v1/shopify/gdpr";

        Map<String, String> m = new LinkedHashMap<>();
        m.put("orders/create", webhooksBase + "/orders/create");
        m.put("orders/updated", webhooksBase + "/orders/updated");
        m.put("customers/create", webhooksBase + "/customers/create");
        m.put("customers/update", webhooksBase + "/customers/update");
        m.put("products/create", webhooksBase + "/products/create");
        m.put("products/update", webhooksBase + "/products/update");
        m.put("app/uninstalled", webhooksBase + "/app/uninstalled");
        m.put("customers/data_request", gdprBase + "/customers/data_request");
        m.put("customers/redact", gdprBase + "/customers/redact");
        m.put("shop/redact", gdprBase + "/shop/redact");
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
        if (topic.contains("data_request") || topic.contains("redact") || topic.equals("orders/create") || topic.equals("customers/create") || topic.equals("customers/update")) {
            return;
        }

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
        } catch (Exception e) {
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
        } catch (Exception e) {
        }
    }

    private record ExistingWebhook(String id, String address) {}
}
