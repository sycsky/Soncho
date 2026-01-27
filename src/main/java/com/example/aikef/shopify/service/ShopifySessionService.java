package com.example.aikef.shopify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShopifySessionService {

    @Value("${shopify.api-key:}")
    private String apiKey;

    @Value("${shopify.api-secret:}")
    private String apiSecret;

    private final ObjectMapper objectMapper;

    public ShopifySessionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VerifiedSession verifySessionToken(String token, String expectedShop) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Missing Shopify API credentials");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSig = hmacSha256(apiSecret, signingInput);
        byte[] providedSig = base64UrlDecode(parts[2]);
        if (!constantTimeEquals(expectedSig, providedSig)) {
            return null;
        }

        try {
            JsonNode payload = objectMapper.readTree(new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8));

            if (!audMatches(payload.get("aud"), apiKey)) {
                return null;
            }

            long now = Instant.now().getEpochSecond();
            long exp = payload.path("exp").asLong(0);
            if (exp <= 0 || exp <= now) {
                return null;
            }
            long nbf = payload.path("nbf").asLong(0);
            if (nbf > 0 && nbf > now) {
                return null;
            }

            String dest = textOrNull(payload.get("dest"));
            String iss = textOrNull(payload.get("iss"));
            String shopDomain = extractShopDomain(dest != null ? dest : iss);
            if (shopDomain == null || shopDomain.isBlank()) {
                return null;
            }

            if (expectedShop != null && !expectedShop.isBlank() && !expectedShop.equalsIgnoreCase(shopDomain)) {
                return null;
            }

            return new VerifiedSession(shopDomain, exp);
        } catch (Exception e) {
            return null;
        }
    }

    public record VerifiedSession(String shopDomain, long exp) {
    }

    private byte[] hmacSha256(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify Shopify session token", e);
        }
    }

    private byte[] base64UrlDecode(String input) {
        return Base64.getUrlDecoder().decode(input);
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private boolean audMatches(JsonNode audNode, String apiKey) {
        if (audNode == null || apiKey == null) {
            return false;
        }
        if (audNode.isArray()) {
            for (JsonNode n : audNode) {
                if (apiKey.equals(n.asText())) {
                    return true;
                }
            }
            return false;
        }
        return apiKey.equals(audNode.asText());
    }

    private String extractShopDomain(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String normalized = url.replace("https://", "").replace("http://", "");
        int slash = normalized.indexOf('/');
        if (slash > -1) {
            normalized = normalized.substring(0, slash);
        }
        return normalized;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text != null && !text.isBlank() ? text : null;
    }
}





