package com.example.aikef.shopify.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShopifyWebhookVerifier {

    @Value("${shopify.api-secret:}")
    private String apiSecret;

    @javax.annotation.PostConstruct
    public void init() {
        if (apiSecret == null || apiSecret.isBlank()) {
            org.slf4j.LoggerFactory.getLogger(ShopifyWebhookVerifier.class).warn("Shopify API Secret is NOT configured! Webhook verification will fail.");
        } else if ("SECRET".equals(apiSecret)) {
            org.slf4j.LoggerFactory.getLogger(ShopifyWebhookVerifier.class).warn("Shopify API Secret is set to default value 'SECRET'. Please configure SPRING_SHOPIFY_API_SECRET environment variable.");
        } else {
            org.slf4j.LoggerFactory.getLogger(ShopifyWebhookVerifier.class).info("Shopify API Secret configured (length: {}).", apiSecret.length());
        }
    }

    public boolean verify(String hmacBase64, byte[] body) {
        if (hmacBase64 == null || hmacBase64.isBlank()) {
            return false;
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Missing shopify.api-secret");
        }
        String expected = calculateHmacBase64(apiSecret, body);
        return constantTimeEquals(expected, hmacBase64);
    }

    private String calculateHmacBase64(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body);
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Webhook HMAC calculation failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}

