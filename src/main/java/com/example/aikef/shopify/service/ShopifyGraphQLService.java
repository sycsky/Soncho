package com.example.aikef.shopify.service;

import com.example.aikef.shopify.model.ShopifyStore;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ShopifyGraphQLService {

    private final ShopifyStoreRepository shopifyStoreRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final com.example.aikef.shopify.repository.ShopifyObjectRepository shopifyObjectRepository;

    @org.springframework.beans.factory.annotation.Value("${shopify.api-version:2025-01}")
    private String apiVersion;

    /**
     * 执行 GraphQL 查询
     * @param query GraphQL 查询语句
     * @param variables 变量
     * @return 响应数据的 data 节点
     */
   
    public JsonNode execute(String query, Map<String, Object> variables) {
        ShopifyStore store = getCurrentStore();
        
        String url = String.format("https://%s/admin/api/%s/graphql.json", store.getShopDomain(), apiVersion);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", store.getAccessToken());

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        if (variables != null) {
            body.put("variables", variables);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Shopify API Error: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.has("errors")) {
                throw new RuntimeException("Shopify Errors: " + root.get("errors").toString());
            }

            return root.get("data");

        } catch (Exception e) {
            log.error("Failed to execute Shopify GraphQL", e);
            throw new RuntimeException("Shopify Query Failed: " + e.getMessage());
        }
    }

    /**
     * Get recent orders for a specific customer
     * @param customerId Shopify Customer ID (can be numeric string or GID)
     * @param limit Number of orders to return
     * @return List of orders
     */
    public JsonNode getOrdersByCustomerId(String customerId, int limit) {
        String gid = customerId;
        if (customerId.matches("\\d+")) {
            gid = "gid://shopify/Customer/" + customerId;
        }

        String query = """
            query($id: ID!, $first: Int!) {
              customer(id: $id) {
                orders(first: $first, sortKey: CREATED_AT, reverse: true) {
                  edges {
                    node {
                      id
                      name
                      createdAt
                      cancelledAt
                      displayFulfillmentStatus
                      totalPriceSet {
                        shopMoney {
                          amount
                          currencyCode
                        }
                      }
                      lineItems(first: 5) {
                        edges {
                          node {
                            title
                            quantity
                            variant {
                              title
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;
            
        Map<String, Object> variables = new HashMap<>();
        variables.put("id", gid);
        variables.put("first", limit);
        
        JsonNode data = execute(query, variables);
        if (data.has("customer") && !data.get("customer").isNull()) {
             return data.get("customer").get("orders").get("edges");
        }
        return objectMapper.createArrayNode();
    }

    /**
     * Use REST API to get policies as they are not easily accessible via Admin GraphQL API
     */
    public JsonNode getPolicies() {
        ShopifyStore store = getCurrentStore();
        String url = String.format("https://%s/admin/api/%s/policies.json", store.getShopDomain(), apiVersion);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", store.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Shopify API Error: " + response.getStatusCode());
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("policies");
        } catch (Exception e) {
            log.error("Failed to get policies via REST", e);
            throw new RuntimeException("Get Policies Failed: " + e.getMessage());
        }
    }

    /**
     * Get checkout via REST API (since it's not available in Admin GraphQL)
     */
    /**
     * Check checkout payment status by looking up associated Order or Checkout in local DB
     * (Since REST API is deprecated and GraphQL doesn't support Checkout type)
     */
    public JsonNode getCheckoutPaymentStatus(String checkoutId) {
        // Normalize ID (remove gid if present)
        String id = checkoutId;
        if (checkoutId.startsWith("gid://shopify/Checkout/")) {
            id = checkoutId.substring("gid://shopify/Checkout/".length());
            if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
        } else if (checkoutId.startsWith("gid://shopify/Order/")) { // Just in case
             id = checkoutId.substring("gid://shopify/Order/".length());
             if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
        }

        ShopifyStore store = getCurrentStore();
        
        // 1. Try to find an ORDER created from this checkout
        // We search for the checkout ID in the order payload (checkout_id field)
        List<com.example.aikef.shopify.model.ShopifyObject> orders = shopifyObjectRepository.findByShopDomainAndObjectTypeAndPayloadJsonContaining(
                store.getShopDomain(),
                com.example.aikef.shopify.model.ShopifyObject.ObjectType.ORDER,
                id // Search for the ID string in JSON
        );
        
        for (com.example.aikef.shopify.model.ShopifyObject orderObj : orders) {
            try {
                JsonNode order = objectMapper.readTree(orderObj.getPayloadJson());
                // Verify it's actually the checkout_id (not some other number)
                if (order.has("checkout_id") && String.valueOf(order.get("checkout_id").asLong()).equals(id)) {
                    // FOUND THE ORDER!
                    return order; // Return the full order object
                }
                 if (order.has("checkout_token") && order.get("checkout_token").asText().equals(id)) {
                    // FOUND THE ORDER via Token!
                    return order; 
                }
            } catch (Exception e) {
                log.warn("Failed to parse order JSON", e);
            }
        }
        
        // 2. If no order found, try to find the CHECKOUT object itself
        java.util.Optional<com.example.aikef.shopify.model.ShopifyObject> checkoutObj = shopifyObjectRepository.findByShopDomainAndObjectTypeAndExternalId(
                store.getShopDomain(), 
                com.example.aikef.shopify.model.ShopifyObject.ObjectType.CHECKOUT, 
                id);
        
        if (checkoutObj.isPresent()) {
            try {
                return objectMapper.readTree(checkoutObj.get().getPayloadJson());
            } catch (Exception e) {
                 log.error("Failed to parse checkout JSON", e);
            }
        }

        // 3. Not found
        return null;
    }

    // Deprecated/Removed REST method
    public JsonNode getCheckout(String checkoutId) {
         return getCheckoutPaymentStatus(checkoutId);
    }

    public ShopifyStore getCurrentStore() {
        // 由于 TenantHibernateFilterAspect 的存在，findAll 只会返回当前租户的 store
        List<ShopifyStore> stores = shopifyStoreRepository.findAll();
        if (stores.isEmpty()) {
            throw new RuntimeException("Current tenant has no linked Shopify store");
        }
        // 理论上每个租户只有一个 Shopify 店铺
        return stores.get(0);
    }
}
