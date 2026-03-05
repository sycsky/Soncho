package com.example.aikef.shopify.service;

import com.example.aikef.shopify.model.ShopifyStore;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyThemeService {

    private final ShopifyStoreRepository storeRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public boolean isAppEmbedEnabled(String shopDomain, String appEmbedHandle) {
        Optional<ShopifyStore> storeOpt = storeRepository.findByShopDomain(shopDomain);
        if (storeOpt.isEmpty()) {
            log.warn("Shop not found: {}", shopDomain);
            return false;
        }

        ShopifyStore store = storeOpt.get();
        String accessToken = store.getAccessToken();

        try {
            // 1. Get Main Theme ID
            String themesUrl = "https://" + shopDomain + "/admin/api/2024-01/themes.json?role=main";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> themesResponse = restTemplate.exchange(themesUrl, HttpMethod.GET, entity, String.class);
            JsonNode themesRoot = objectMapper.readTree(themesResponse.getBody());
            JsonNode themes = themesRoot.path("themes");

            if (themes.isEmpty()) {
                log.warn("No main theme found for shop: {}", shopDomain);
                return false;
            }

            long themeId = themes.get(0).get("id").asLong();

            // 2. Get settings_data.json
            String assetUrl = "https://" + shopDomain + "/admin/api/2024-01/themes/" + themeId + "/assets.json?asset[key]=config/settings_data.json";
            ResponseEntity<String> assetResponse = restTemplate.exchange(assetUrl, HttpMethod.GET, entity, String.class);
            JsonNode assetRoot = objectMapper.readTree(assetResponse.getBody());
            String jsonValue = assetRoot.path("asset").path("value").asText();

            if (jsonValue == null || jsonValue.isEmpty()) {
                return false;
            }

            JsonNode settingsData = objectMapper.readTree(jsonValue);
            JsonNode current = settingsData.path("current");
            JsonNode blocks = current.path("blocks");

            if (blocks.isMissingNode()) {
                return false;
            }

            // 3. Check for App Embed Block
            // Iterate over all blocks to find one with type containing the handle
            // The key is a UUID, the value is the block config
            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = blocks.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> field = fields.next();
                JsonNode block = field.getValue();
                String type = block.path("type").asText("");
                boolean disabled = block.path("disabled").asBoolean(false);

                // Type format: shopify://apps/{app-handle}/blocks/{block-handle}/{uuid}
                if (type.contains(appEmbedHandle) && !disabled) {
                    return true;
                }
            }

        } catch (Exception e) {
            log.error("Error checking app embed status for shop: {}", shopDomain, e);
        }

        return false;
    }
}
