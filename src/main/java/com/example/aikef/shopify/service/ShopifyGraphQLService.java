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
    @Transactional(readOnly = true)
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
                throw new RuntimeException("Shopify GraphQL Errors: " + root.get("errors").toString());
            }

            return root.get("data");

        } catch (Exception e) {
            log.error("Failed to execute Shopify GraphQL", e);
            throw new RuntimeException("Shopify Query Failed: " + e.getMessage());
        }
    }

    private ShopifyStore getCurrentStore() {
        // 由于 TenantHibernateFilterAspect 的存在，findAll 只会返回当前租户的 store
        List<ShopifyStore> stores = shopifyStoreRepository.findAll();
        if (stores.isEmpty()) {
            throw new RuntimeException("Current tenant has no linked Shopify store");
        }
        // 理论上每个租户只有一个 Shopify 店铺
        return stores.get(0);
    }
}
