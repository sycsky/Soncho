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
public class ShopifyGraphQLService {

    private final ShopifyStoreRepository shopifyStoreRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
