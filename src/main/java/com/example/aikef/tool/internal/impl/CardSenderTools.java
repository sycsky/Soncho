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
                    String variantId = node.path("id").asText();
                    if (variantId.startsWith("gid://shopify/ProductVariant/")) {
                        variantId = variantId.substring(variantId.lastIndexOf('/') + 1);
                    }
                    cardData.put("variantId", variantId);
                    
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
                        String variantId = firstVariant.path("id").asText();
                        if (variantId.startsWith("gid://shopify/ProductVariant/")) {
                            variantId = variantId.substring(variantId.lastIndexOf('/') + 1);
                        }
                        cardData.put("variantId", variantId);
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

            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cards);
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
            
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cardData);
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
            
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cardData);
            return "card#CARD_DISCOUNT#" + payload;
        } catch (Exception e) {
            return "Failed to create discount card.";
        }
    }

    @Tool("Send an order card to the user with order details and tracking information.")
    public String sendOrderCard(
            @P("The Shopify Order Number (e.g., '#1001' or '1001')") String orderNumber,
            @P("Customer email address for verification") String email) {
        try {
            if (orderNumber == null || orderNumber.isBlank()) {
                return "Order number is required.";
            }
            if (email == null || email.isBlank()) {
                return "Email is required for order verification.";
            }

            // Get order details from Shopify
            String orderDetailsJson = shopifyTools.getOrderDetails(orderNumber, email);
            
            // Check if it's an error message
            if (orderDetailsJson.contains("Order not found") || 
                orderDetailsJson.contains("Email does not match") ||
                orderDetailsJson.contains("订单不存在") ||
                orderDetailsJson.contains("邮箱不匹配")) {
                return orderDetailsJson; // Return error as text message
            }

            // Parse the order details (GraphQL structure)
            JsonNode order = objectMapper.readTree(orderDetailsJson);
            
            Map<String, Object> cardData = new HashMap<>();
            
            // Basic order info
            cardData.put("orderNumber", order.path("name").asText());
            cardData.put("orderId", order.path("id").asText());
            
            // Price info
            JsonNode totalPriceSet = order.path("totalPriceSet").path("shopMoney");
            // Use currentTotalPriceSet if available (reflects edits)
            if (order.has("currentTotalPriceSet") && !order.get("currentTotalPriceSet").isNull()) {
                 JsonNode currentShopMoney = order.get("currentTotalPriceSet").get("shopMoney");
                 if (currentShopMoney != null && !currentShopMoney.isNull()) {
                     totalPriceSet = currentShopMoney;
                 }
            }
            cardData.put("totalPrice", totalPriceSet.path("amount").asText());
            cardData.put("currency", totalPriceSet.path("currencyCode").asText("USD"));
            
            // Status
            cardData.put("financialStatus", order.path("displayFinancialStatus").asText());
            cardData.put("fulfillmentStatus", order.path("displayFulfillmentStatus").asText());
            cardData.put("createdAt", order.path("createdAt").asText());
            
            // Note
            String note = order.path("note").asText("");
            if (!note.isEmpty()) {
                cardData.put("note", note);
            }
            
            // Customer info
            JsonNode customer = order.path("customer");
            if (!customer.isMissingNode()) {
                Map<String, String> customerData = new HashMap<>();
                customerData.put("firstName", customer.path("firstName").asText());
                customerData.put("lastName", customer.path("lastName").asText());
                customerData.put("email", customer.path("email").asText());
                customerData.put("phone", customer.path("phone").asText());
                cardData.put("customer", customerData);
            }
            
            // Shipping address
            JsonNode shippingAddress = order.path("shippingAddress");
            if (!shippingAddress.isMissingNode() && !shippingAddress.isNull()) {
                Map<String, String> addressData = new HashMap<>();
                addressData.put("name", shippingAddress.path("name").asText());
                addressData.put("firstName", shippingAddress.path("firstName").asText());
                addressData.put("lastName", shippingAddress.path("lastName").asText());
                addressData.put("phone", shippingAddress.path("phone").asText());
                addressData.put("address1", shippingAddress.path("address1").asText());
                addressData.put("address2", shippingAddress.path("address2").asText());
                addressData.put("city", shippingAddress.path("city").asText());
                addressData.put("province", shippingAddress.path("province").asText());
                addressData.put("country", shippingAddress.path("country").asText());
                addressData.put("zip", shippingAddress.path("zip").asText());
                cardData.put("shippingAddress", addressData);
            }
            
            // Add tracking info from fulfillments
            JsonNode fulfillments = order.path("fulfillments");
            if (fulfillments.isArray() && fulfillments.size() > 0) {
                java.util.List<Map<String, String>> trackingList = new java.util.ArrayList<>();
                for (JsonNode fulfillment : fulfillments) {
                    JsonNode trackingInfoArray = fulfillment.path("trackingInfo");
                    if (trackingInfoArray.isArray()) {
                        for (JsonNode tracking : trackingInfoArray) {
                            Map<String, String> trackingData = new HashMap<>();
                            trackingData.put("number", tracking.path("number").asText());
                            trackingData.put("url", tracking.path("url").asText());
                            trackingData.put("company", tracking.path("company").asText());
                            trackingList.add(trackingData);
                        }
                    }
                }
                if (!trackingList.isEmpty()) {
                    cardData.put("trackingInfo", trackingList);
                }
            }
            
            // Add line items
            JsonNode lineItemsEdges = order.path("lineItems").path("edges");
            if (lineItemsEdges.isArray() && lineItemsEdges.size() > 0) {
                java.util.List<Map<String, Object>> itemsList = new java.util.ArrayList<>();
                for (JsonNode edge : lineItemsEdges) {
                    JsonNode item = edge.path("node");
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("title", item.path("title").asText());
                    itemData.put("quantity", item.path("quantity").asInt());
                    itemData.put("price", item.path("originalUnitPriceSet").path("shopMoney").path("amount").asText());
                    if (item.has("variantTitle") && !item.get("variantTitle").isNull()) {
                        itemData.put("variantTitle", item.path("variantTitle").asText());
                    }
                    if (item.has("variant") && !item.get("variant").isNull()) {
                        String vId = item.path("variant").path("id").asText();
                        if (vId.startsWith("gid://shopify/ProductVariant/")) {
                            vId = vId.substring(vId.lastIndexOf('/') + 1);
                        }
                        itemData.put("variantId", vId);
                    }
                    itemsList.add(itemData);
                }
                cardData.put("items", itemsList);
            }
            
            // Wrap in array for consistent format
            java.util.List<Map<String, Object>> cardDataList = new java.util.ArrayList<>();
            cardDataList.add(cardData);
            
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cardDataList);
            return "card#CARD_ORDER#" + payload;
        } catch (Exception e) {
            log.error("Failed to create order card", e);
            return "Failed to retrieve order information: " + e.getMessage();
        }
    }

    @Tool("Search orders by email and send order cards to the user.")
    public String sendOrderCardsByEmail(
            @P("Customer email address") String email) {
        try {
            if (email == null || email.isBlank()) {
                return "Email is required.";
            }

            // Search orders by email
            String ordersJson = shopifyTools.searchOrdersByEmail(email);
            
            // Check if it's an error message
            if (ordersJson.contains("No orders found") || 
                ordersJson.contains("未找到") ||
                ordersJson.contains("Failed to search")) {
                return ordersJson; // Return error as text message
            }

            // Parse the orders list (GraphQL edges format)
            JsonNode edges = objectMapper.readTree(ordersJson);
            
            if (!edges.isArray() || edges.size() == 0) {
                return String.format("No orders found for email: %s", email);
            }

            java.util.List<Map<String, Object>> orderCards = new java.util.ArrayList<>();
            
            for (JsonNode edge : edges) {
                JsonNode order = edge.path("node");
                
                Map<String, Object> cardData = new HashMap<>();
                
                // Basic order info
                cardData.put("orderNumber", order.path("name").asText());
                cardData.put("orderId", order.path("id").asText());
                
                // Price info
                JsonNode totalPriceSet = order.path("totalPriceSet").path("shopMoney");
                // Use currentTotalPriceSet if available (reflects edits)
                if (order.has("currentTotalPriceSet") && !order.get("currentTotalPriceSet").isNull()) {
                     JsonNode currentShopMoney = order.get("currentTotalPriceSet").get("shopMoney");
                     if (currentShopMoney != null && !currentShopMoney.isNull()) {
                         totalPriceSet = currentShopMoney;
                     }
                }
                cardData.put("totalPrice", totalPriceSet.path("amount").asText());
                cardData.put("currency", totalPriceSet.path("currencyCode").asText("USD"));
                
                // Status
                cardData.put("financialStatus", order.path("displayFinancialStatus").asText());
                cardData.put("fulfillmentStatus", order.path("displayFulfillmentStatus").asText());
                cardData.put("createdAt", order.path("createdAt").asText());
                
                // Note
                String note = order.path("note").asText("");
                if (!note.isEmpty()) {
                    cardData.put("note", note);
                }
                
                // Shipping address
                JsonNode shippingAddress = order.path("shippingAddress");
                if (!shippingAddress.isMissingNode() && !shippingAddress.isNull()) {
                    Map<String, String> addressData = new HashMap<>();
                    addressData.put("name", shippingAddress.path("name").asText());
                    addressData.put("firstName", shippingAddress.path("firstName").asText());
                    addressData.put("lastName", shippingAddress.path("lastName").asText());
                    addressData.put("phone", shippingAddress.path("phone").asText());
                    addressData.put("address1", shippingAddress.path("address1").asText());
                    addressData.put("address2", shippingAddress.path("address2").asText());
                    addressData.put("city", shippingAddress.path("city").asText());
                    addressData.put("province", shippingAddress.path("province").asText());
                    addressData.put("country", shippingAddress.path("country").asText());
                    addressData.put("zip", shippingAddress.path("zip").asText());
                    cardData.put("shippingAddress", addressData);
                }
                
                // Line items (simplified from edges format)
                JsonNode lineItemsEdges = order.path("lineItems").path("edges");
                if (lineItemsEdges.isArray() && lineItemsEdges.size() > 0) {
                    java.util.List<Map<String, Object>> itemsList = new java.util.ArrayList<>();
                    for (JsonNode itemEdge : lineItemsEdges) {
                        JsonNode item = itemEdge.path("node");
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("title", item.path("title").asText());
                        itemData.put("quantity", item.path("quantity").asInt());
                        if (item.has("variantTitle") && !item.get("variantTitle").isNull()) {
                            itemData.put("variantTitle", item.path("variantTitle").asText());
                        }
                        if (item.has("variant") && !item.get("variant").isNull()) {
                            String vId = item.path("variant").path("id").asText();
                            if (vId.startsWith("gid://shopify/ProductVariant/")) {
                                vId = vId.substring(vId.lastIndexOf('/') + 1);
                            }
                            itemData.put("variantId", vId);
                        }
                        itemsList.add(itemData);
                    }
                    cardData.put("items", itemsList);
                }
                
                orderCards.add(cardData);
            }
            
            if (orderCards.isEmpty()) {
                return String.format("No valid orders found for email: %s", email);
            }
            
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderCards);
            return "card#CARD_ORDER#" + payload;
            
        } catch (Exception e) {
            log.error("Failed to create order cards from email", e);
            return "Failed to retrieve orders: " + e.getMessage();
        }
    }
}
