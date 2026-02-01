package com.example.aikef.shopify.controller;

import com.example.aikef.model.PermissionConstants;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SentItemType;
import com.example.aikef.service.SentItemService;
import com.example.aikef.tool.internal.impl.ShopifyCustomerServiceTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/shopify")
@RequiredArgsConstructor
@Slf4j
public class ShopifyProductController {

    private final ShopifyCustomerServiceTools shopifyTools;
    private final SentItemService sentItemService;
    private final ObjectMapper objectMapper;

    /**
     * 获取店铺商品列表（支持关键词搜索和分页），并关联折扣活动
     */
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCESS_SHOPIFY_PRODUCTS + "')")
    public JsonNode getProducts(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor
    ) {
        try {
            // 1. Fetch products
            String productsJson = shopifyTools.searchProductsWithPagination(query, limit, cursor);
            JsonNode productsNode = objectMapper.readTree(productsJson);
            
            // 2. Fetch active price rules
            String rulesJson = shopifyTools.getActivePriceRules();
            JsonNode rulesNode = objectMapper.readTree(rulesJson);
            
            // 3. Enrich products with applicable discounts
            if (productsNode.has("edges") && productsNode.get("edges").isArray()) {
                for (JsonNode edge : productsNode.get("edges")) {
                    JsonNode product = edge.get("node");
                    if (product instanceof ObjectNode) {
                        List<String> applicableDiscounts = new ArrayList<>();
                        String productId = product.get("id").asText();
                        List<String> productCollectionIds = new ArrayList<>();
                        if (product.has("collections") && product.get("collections").has("edges")) {
                            for (JsonNode cEdge : product.get("collections").get("edges")) {
                                productCollectionIds.add(cEdge.get("node").get("id").asText());
                            }
                        }

                        if (rulesNode.has("edges")) {
                            for (JsonNode ruleEdge : rulesNode.get("edges")) {
                                JsonNode discountNode = ruleEdge.get("node");
                                JsonNode discount = discountNode.get("discount");
                                
                                // Handle basic code discounts and automatic basic discounts
                                // Structure is slightly different but we look for title and customerGets->items
                                String title = discount.has("title") ? discount.get("title").asText() : "";
                                if (discount.has("codes") && discount.get("codes").has("nodes") && discount.get("codes").get("nodes").isArray() && discount.get("codes").get("nodes").size() > 0) {
                                    // Use the actual code instead of title if available
                                    title = discount.get("codes").get("nodes").get(0).get("code").asText();
                                }
                                
                                boolean applies = false;

                                if (discount.has("customerGets") && discount.get("customerGets").has("items")) {
                                    JsonNode items = discount.get("customerGets").get("items");
                                    
                                    // Check if it applies to products
                                    if (items.has("products") && items.get("products").has("edges")) {
                                        for (JsonNode pEdge : items.get("products").get("edges")) {
                                            if (productId.equals(pEdge.get("node").get("id").asText())) {
                                                applies = true;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    // Check if it applies to product variants (indirectly applies to the product)
                                    if (!applies && items.has("productVariants") && items.get("productVariants").has("edges")) {
                                        for (JsonNode vEdge : items.get("productVariants").get("edges")) {
                                            JsonNode variantNode = vEdge.get("node");
                                            if (variantNode.has("product") && variantNode.get("product").has("id")) {
                                                if (productId.equals(variantNode.get("product").get("id").asText())) {
                                                    applies = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Check if it applies to collections
                                    if (!applies && items.has("collections") && items.get("collections").has("edges")) {
                                        for (JsonNode cEdge : items.get("collections").get("edges")) {
                                            if (productCollectionIds.contains(cEdge.get("node").get("id").asText())) {
                                                applies = true;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    // If items doesn't specify products/collections, it might apply to all? 
                                    // In GraphQL BasicDiscountItems, if 'all' is true (not exposed in our query above but logic wise), 
                                    // usually products/collections are null or empty if it applies to everything. 
                                    // But strictly speaking we should check 'allItems' field if we queried it.
                                    // For now, let's assume if we queried products/collections and they are empty/null, it MIGHT be store-wide 
                                    // BUT usually store-wide discounts don't have this structure or we need to check differently.
                                    // Let's stick to explicit product/collection matching for safety.
                                }

                                if (applies && !title.isEmpty()) {
                                    applicableDiscounts.add(title);
                                }
                            }
                        }
                        
                        ArrayNode discountsArray = objectMapper.createArrayNode();
                        applicableDiscounts.forEach(discountsArray::add);
                        ((ObjectNode) product).set("applicableDiscounts", discountsArray);
                    }
                }
            }

            return productsNode;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse result", e);
            throw new RuntimeException("Result parsing failed");
        } catch (Exception e) {
            log.error("Failed to search products", e);
            throw new RuntimeException("Product search failed");
        }
    }

    /**
     * 获取所有活跃的折扣活动
     */
    @GetMapping("/discounts")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCESS_SHOPIFY_DISCOUNTS + "')")
    public JsonNode getActiveDiscounts() {
        try {
            String jsonResult = shopifyTools.getActivePriceRules();
            JsonNode root = objectMapper.readTree(jsonResult);
            return filterSendableDiscounts(root);
        } catch (Exception e) {
            log.error("Failed to get discounts", e);
            throw new RuntimeException("Get discounts failed");
        }
    }

    private JsonNode filterSendableDiscounts(JsonNode root) {
        ArrayNode filteredEdges = objectMapper.createArrayNode();
        Instant now = Instant.now();

        if (root != null && root.has("edges") && root.get("edges").isArray()) {
            for (JsonNode edge : root.get("edges")) {
                JsonNode discount = edge.path("node").path("discount");
                if (isSendableDiscount(discount, now)) {
                    filteredEdges.add(edge);
                }
            }
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("edges", filteredEdges);
        return result;
    }

    private boolean isSendableDiscount(JsonNode discount, Instant now) {
        if (discount == null || discount.isNull()) {
            return false;
        }

        String type = discount.path("__typename").asText("");
        if (!type.startsWith("DiscountCode")) {
            return false;
        }

        JsonNode codes = discount.path("codes").path("nodes");
        if (!codes.isArray() || codes.size() == 0) {
            return false;
        }

        String code = codes.get(0).path("code").asText("").trim();
        if (code.isEmpty()) {
            return false;
        }

        String startsAt = discount.path("startsAt").asText(null);
        if (startsAt != null && !startsAt.isBlank()) {
            try {
                if (Instant.parse(startsAt).isAfter(now)) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }

        String endsAt = discount.path("endsAt").asText(null);
        if (endsAt != null && !endsAt.isBlank()) {
            try {
                if (!Instant.parse(endsAt).isAfter(now)) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    /**
     * 创建礼品卡
     */
    @PostMapping("/gift-cards")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_SHOPIFY_GIFT_CARDS + "')")
    public JsonNode createGiftCard(@RequestBody Map<String, String> payload) {
        try {
            String amount = payload.get("amount");
            String note = payload.getOrDefault("note", "Created by Agent");
            String customerId = payload.get("customerId");
            String expiresOn = payload.get("expiresOn");
            
            String jsonResult = shopifyTools.createGiftCard(amount, note, customerId, SenderType.AGENT, expiresOn);
            return objectMapper.readTree(jsonResult);
        } catch (Exception e) {
            log.error("Failed to create gift card", e);
            throw new RuntimeException("Create gift card failed");
        }
    }

    /**
     * 记录发送的物品（如折扣码）
     */
    @PostMapping("/sent-items")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCESS_SHOPIFY_DISCOUNTS + "') or hasAuthority('" + PermissionConstants.MANAGE_SHOPIFY_GIFT_CARDS + "')")
    public void recordSentItem(@RequestBody Map<String, String> payload) {
        try {
            String customerId = payload.get("customerId");
            String typeStr = payload.get("itemType");
            String itemValue = payload.get("itemValue");
            String amount = payload.get("amount");
            String note = payload.get("note");
            
            if (customerId == null || typeStr == null || itemValue == null) {
                throw new IllegalArgumentException("Missing required fields");
            }

            SentItemType itemType = SentItemType.valueOf(typeStr);
            
            sentItemService.recordSentItem(
                customerId,
                itemType,
                itemValue,
                amount,
                SenderType.AGENT,
                note
            );
        } catch (Exception e) {
            log.error("Failed to record sent item", e);
            // Don't throw to frontend to avoid disrupting the chat flow, just log
        }
    }
}
