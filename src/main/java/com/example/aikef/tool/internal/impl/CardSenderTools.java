package com.example.aikef.tool.internal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardSenderTools {

    private final ShopifyCustomerServiceTools shopifyTools;
    private final ObjectMapper objectMapper;

    @Tool("Send product cards to the user. Supports multiple products or variants.")
    public String sendProductCard(
            @P("Comma-separated list of Shopify Product IDs or Variant IDs (e.g. '12345,67890')") String ids) {
        try {
            if (ids == null || ids.isBlank()) {
                return "No product IDs provided.";
            }

            // Parse IDs and ensure GID format
            java.util.List<String> gidList = new java.util.ArrayList<>();
            for (String id : ids.split(",")) {
                String trimmedId = id.trim();
                if (trimmedId.isEmpty()) continue;
                
                // Heuristic: If it looks like a Product ID (short numeric) vs Variant ID (long numeric) is hard to tell.
                // But usually we assume Product ID if not specified.
                // However, the new tool supports polymorphic IDs.
                // We'll try to guess or just prepend generic ID if missing.
                // Ideally, the agent provides full GIDs or we try both.
                // For simplicity, if it doesn't start with gid://, we assume Product ID.
                // BUT, user asked for Variant ID support.
                // If the agent is smart, it might provide context. 
                // Let's assume the Agent provides whatever it found from search tools.
                // Search tools return numeric IDs usually (from our implementation).
                // Let's assume they are Product IDs by default unless context implies otherwise?
                // Actually, our `searchProducts` returns Product GIDs (or numeric IDs in node).
                // Let's try to handle numeric IDs as Product IDs by default, 
                // but if we want to support Variants, we might need a way to distinguish.
                // Let's assume the Agent sends GIDs if it has them.
                // If numeric, default to Product.
                
                if (trimmedId.startsWith("gid://")) {
                    gidList.add(trimmedId);
                } else {
                    // Default to Product if just a number
                    gidList.add("gid://shopify/Product/" + trimmedId);
                }
            }

            if (gidList.isEmpty()) {
                return "No valid IDs found.";
            }

            JsonNode nodes = shopifyTools.getProductsOrVariantsDetails(gidList);
            java.util.List<Map<String, Object>> cards = new java.util.ArrayList<>();

            for (JsonNode node : nodes) {
                if (node == null || node.isNull()) continue;
                
                Map<String, Object> cardData = new HashMap<>();
                String type = node.path("__typename").asText();
                
                if ("ProductVariant".equals(type)) {
                    // Handle Variant
                    JsonNode product = node.path("product");
                    cardData.put("id", product.path("id").asText()); // Use Product ID for the main link usually, or keep variant context
                    cardData.put("title", product.path("title").asText() + " - " + node.path("title").asText());
                    cardData.put("handle", product.path("handle").asText());
                    cardData.put("price", node.path("price").asText());
                    cardData.put("variantId", node.path("id").asText());
                    
                    // Image: Variant image -> Product featured image
                    String imgUrl = node.path("image").path("url").asText();
                    if (imgUrl.isEmpty()) {
                        imgUrl = product.path("featuredImage").path("url").asText();
                    }
                    cardData.put("image", imgUrl);
                    
                    // Currency
                    cardData.put("currency", product.path("priceRange").path("minVariantPrice").path("currencyCode").asText("USD"));
                    
                } else if ("Product".equals(type)) {
                    // Handle Product
                    cardData.put("id", node.path("id").asText());
                    cardData.put("title", node.path("title").asText());
                    cardData.put("handle", node.path("handle").asText());
                    
                    // Price & Variant ID from first variant
                    JsonNode variants = node.path("variants").path("edges");
                    if (variants.isArray() && variants.size() > 0) {
                        JsonNode firstVariant = variants.get(0).path("node");
                        cardData.put("price", firstVariant.path("price").asText());
                        cardData.put("variantId", firstVariant.path("id").asText());
                    } else {
                         cardData.put("price", node.path("priceRange").path("minVariantPrice").path("amount").asText());
                    }
                    
                    cardData.put("image", node.path("featuredImage").path("url").asText());
                    cardData.put("currency", node.path("priceRange").path("minVariantPrice").path("currencyCode").asText("USD"));
                }
                
                cards.add(cardData);
            }

            if (cards.isEmpty()) {
                return "Failed to fetch details for provided IDs.";
            }

            String payload = objectMapper.writeValueAsString(cards);
            return "card#CARD_PRODUCT#" + payload;

        } catch (Exception e) {
            log.error("Failed to construct product card", e);
            return "Failed to find product information: " + e.getMessage();
        }
    }

    @Tool("Send a gift card to the user.")
    public String sendGiftCard(@P("Amount/Value of the gift card") String amount) {
        try {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("amount", amount);
            cardData.put("code", "GIFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            
            String payload = objectMapper.writeValueAsString(cardData);
            return "card#CARD_GIFT#" + payload;
        } catch (Exception e) {
            return "Failed to create gift card.";
        }
    }

    @Tool("Send a discount card to the user.")
    public String sendDiscountCard(
            @P("Discount code") String code,
            @P("Discount value (e.g. '20% off' or '$10')") String value) {
        try {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("code", code);
            cardData.put("value", value);
            
            String payload = objectMapper.writeValueAsString(cardData);
            return "card#CARD_DISCOUNT#" + payload;
        } catch (Exception e) {
            return "Failed to create discount card.";
        }
    }
}
