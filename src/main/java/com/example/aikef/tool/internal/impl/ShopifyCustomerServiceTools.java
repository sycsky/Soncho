package com.example.aikef.tool.internal.impl;

import cn.hutool.json.JSONObject;
import com.example.aikef.model.Customer;
import com.example.aikef.model.OrderCancellationPolicy;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.service.EmailVerificationService;
import com.example.aikef.service.OrderCancellationPolicyService;
import com.example.aikef.shopify.service.ShopifyGraphQLService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShopifyCustomerServiceTools {

    private final ShopifyGraphQLService graphQLService;
    private final EmailVerificationService emailVerificationService;
    private final CustomerRepository customerRepository;
    private final OrderCancellationPolicyService cancellationPolicyService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String PRODUCTS_CACHE_KEY = "shopify:all_products:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10); // 10分钟缓存

    private void filterOrderLineItems(JsonNode orderNode) {
        if (!(orderNode instanceof ObjectNode)) return;
        
        ObjectNode order = (ObjectNode) orderNode;
        JsonNode lineItemsNode = order.get("lineItems");
        if (lineItemsNode == null || !(lineItemsNode instanceof ObjectNode)) return;
        
        ObjectNode lineItemsObj = (ObjectNode) lineItemsNode;
        JsonNode edgesNode = lineItemsObj.get("edges");
        if (edgesNode == null || !(edgesNode instanceof ArrayNode)) return;
        
        ArrayNode edges = (ArrayNode) edgesNode;
        ArrayNode filteredEdges = objectMapper.createArrayNode();
        
        for (JsonNode edge : edges) {
            JsonNode node = edge.path("node");
            // 优先使用 currentQuantity (当前数量)，如果不存在则回退到 quantity (原始数量)
            int currentQuantity = node.has("currentQuantity") 
                ? node.path("currentQuantity").asInt(0) 
                : node.path("quantity").asInt(0);
            
            if (currentQuantity > 0) {
                // 更新 quantity 字段为 currentQuantity，以便前端显示正确的当前数量
                if (node instanceof ObjectNode) {
                    ((ObjectNode) node).put("quantity", currentQuantity);
                }

                // 简化变体 ID
                JsonNode variant = node.path("variant");
                if (variant instanceof ObjectNode) {
                    String vId = variant.path("id").asText();
                    if (vId.startsWith("gid://shopify/ProductVariant/")) {
                        ((ObjectNode) variant).put("id", vId.substring(vId.lastIndexOf('/') + 1));
                    }
                }
                filteredEdges.add(edge);
            }
        }
        
        lineItemsObj.set("edges", filteredEdges);
    }

    @Tool("Search for orders by email, order number (e.g. '#1001'), or customer name")
    public String searchOrders(@P(value = "Query string (email, order name, or syntax like 'financial_status:paid')", required = true) String query) {
        String gql = """
            query ($query: String!) {
              orders(first: 5, query: $query, sortKey: CREATED_AT, reverse: true) {
                edges {
                  node {
                    id
                    name
                    createdAt
                    displayFinancialStatus
                    displayFulfillmentStatus
                    totalPriceSet {
                      shopMoney {
                        amount
                        currencyCode
                      }
                    }
                    customer {
                      firstName
                      lastName
                      email
                    }
                    lineItems(first: 50) {
                      edges {
                        node {
                          title
                          quantity
                          currentQuantity
                          variantTitle
                          variant {
                            id
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;
        
        try {
            JsonNode data = graphQLService.execute(gql, Map.of("query", query));
            JsonNode edges = data.path("orders").path("edges");
            
            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                    filterOrderLineItems(edge.path("node"));
                }
            }
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edges);
        } catch (Exception e) {
            log.error("Search orders failed", e);
            return "Search failed: " + e.getMessage();
        }
    }

    @Tool("Get detailed information about a specific order by Order Number with email verification")
    public String getOrderDetails(
            @P(value = "The Shopify Order Number (e.g., #1001 or 1001)", required = true) String orderNumber,
            @P(value = "Customer email address for verification", required = true) String email) {
        
        log.info("查询订单详情: orderNumber={}, email={}", orderNumber, email);
        
        // 验证必填参数
        if (email == null || email.isBlank()) {
            return "Email is required for order verification / 查询订单需要提供邮箱地址";
        }
        
        // 处理订单号格式：自动添加 # 前缀（如果没有）
        String searchOrderNumber = orderNumber.trim();
        if (!searchOrderNumber.startsWith("#")) {
            searchOrderNumber = "#" + searchOrderNumber;
        }

        // 使用订单号搜索订单
        String gql = """
            query ($query: String!) {
              orders(first: 1, query: $query) {
                edges {
                  node {
                    id
                    name
                    createdAt
                    note
                    email
                    totalPriceSet {
                      shopMoney {
                        amount
                        currencyCode
                      }
                    }
                    displayFinancialStatus
                    displayFulfillmentStatus
                    shippingAddress {
                      name
                      firstName
                      lastName
                      phone
                      address1
                      address2
                      city
                      province
                      country
                      zip
                    }
                    customer {
                      firstName
                      lastName
                      email
                      phone
                    }
                    lineItems(first: 50) {
                      edges {
                        node {
                          title
                          quantity
                          currentQuantity
                          sku
                          variantTitle
                          variant {
                            id
                          }
                          originalUnitPriceSet {
                            shopMoney {
                              amount
                              currencyCode
                            }
                          }
                        }
                      }
                    }
                    fulfillments(first: 10) {
                      status
                      trackingInfo(first: 5) {
                        number
                        url
                        company
                      }
                    }
                  }
                }
              }
            }
            """;

        try {
            // 使用订单号作为查询条件
            String query = "name:" + searchOrderNumber;
            JsonNode data = graphQLService.execute(gql, Map.of("query", query));
            JsonNode edges = data.get("orders").get("edges");
            
            // 检查是否找到订单
            if (edges == null || !edges.isArray() || edges.size() == 0) {
                log.warn("订单未找到: orderNumber={}", orderNumber);
                return String.format("Order not found / 订单不存在: %s", searchOrderNumber);
            }
            
            JsonNode order = edges.get(0).get("node");
            
            // 验证订单邮箱与提供的邮箱是否匹配
            String orderEmail = order.get("email").asText("");
            if (!email.equalsIgnoreCase(orderEmail)) {
                log.warn("订单邮箱不匹配: orderNumber={}, 提供的email={}, 订单email={}", 
                        orderNumber, email, orderEmail);
                return String.format(
                    "Order email does not match. Access denied / 订单邮箱不匹配，无法访问此订单（提供: %s, 订单: %s）",
                    email, orderEmail
                );
            }
            
            log.info("订单详情查询成功: orderNumber={}, email={}", orderNumber, email);
            
            // 过滤已移除商品（数量为 0）并简化变体 ID
            filterOrderLineItems(order);

            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);
            } catch (Exception e) {
                return order.toString();
            }
            
        } catch (Exception e) {
            log.error("查询订单详情失败: orderNumber={}, email={}", orderNumber, email, e);
            return "Failed to get order details / 查询订单详情失败: " + e.getMessage();
        }
    }

    @Tool("Search orders by customer email address")
    public String searchOrdersByEmail(
            @P(value = "Customer email address", required = true) String email) {
        
        log.info("通过邮箱查询订单列表: email={}", email);
        
        // 验证必填参数
        if (email == null || email.isBlank()) {
            return "Email is required / 查询订单需要提供邮箱地址";
        }
        
        // 使用邮箱搜索订单
        String gql = """
            query ($query: String!) {
              orders(first: 10, query: $query, sortKey: CREATED_AT, reverse: true) {
                edges {
                  node {
                    id
                    name
                    createdAt
                    email
                    totalPriceSet {
                      shopMoney {
                        amount
                        currencyCode
                      }
                    }
                    displayFinancialStatus
                    displayFulfillmentStatus
                    shippingAddress {
                      name
                      firstName
                      lastName
                      phone
                      address1
                      address2
                      city
                      province
                      country
                      zip
                    }
                    customer {
                      firstName
                      lastName
                      email
                      phone
                    }
                    lineItems(first: 50) {
                      edges {
                        node {
                          title
                          quantity
                          currentQuantity
                          variantTitle
                          variant {
                            id
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        try {
            // 使用邮箱作为查询条件
            String query = "email:" + email;
            JsonNode data = graphQLService.execute(gql, Map.of("query", query));
            JsonNode edges = data.get("orders").get("edges");
            
            // 检查是否找到订单
            if (edges == null || !edges.isArray() || edges.size() == 0) {
                log.info("未找到该邮箱的订单: email={}", email);
                return String.format("No orders found for email: %s / 未找到该邮箱的订单", email);
            }
            
            log.info("找到 {} 个订单: email={}", edges.size(), email);
            
            // 过滤已移除商品（数量为 0）并简化变体 ID
            if (edges.isArray()) {
                for (JsonNode edge : edges) {
                    filterOrderLineItems(edge.path("node"));
                }
            }
            
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(edges);
            } catch (Exception e) {
                return edges.toString();
            }
            
        } catch (Exception e) {
            log.error("通过邮箱查询订单失败: email={}", email, e);
            return "Failed to search orders / 查询订单失败: " + e.getMessage();
        }
    }

    @Tool("Search for products by keyword")
    public String searchProducts(@P(value = "Search keyword (title, sku, etc.)", required = false) String query) {
        // Delegate to the pagination method with defaults for the AI tool
        if (isNotEmpty(query)) {
            return searchProductsWithPagination(query, 5, null);
        }
        return getAllProducts();
    }
    
    @Tool("Get all products from the store (cached for 10 minutes)")
    public String getAllProducts() {
        try {
            // 1. 构建缓存键（包含租户信息以实现多租户隔离）
            String tenantId = getCurrentTenantId();
            String cacheKey = PRODUCTS_CACHE_KEY + tenantId;
            
            // 2. 尝试从缓存获取
//            String cachedData = redisTemplate.opsForValue().get(cacheKey);
//            if (cachedData != null && !cachedData.isEmpty()) {
//                log.debug("从缓存返回所有商品数据: tenantId={}", tenantId);
//                return cachedData;
//            }
            
            // 3. 缓存未命中，从 Shopify API 获取所有商品
            log.info("缓存未命中，从 Shopify API 获取所有商品: tenantId={}", tenantId);
            List<JsonNode> allProducts = new ArrayList<>();
            String cursor = null;
            int pageSize = 50; // 每页50个商品
            int maxPages = 20; // 最多获取20页（1000个商品）
            int pageCount = 0;
            
            do {
                String gql = """
                    query ($first: Int!, $after: String) {
                      products(first: $first, after: $after, sortKey: TITLE) {
                        pageInfo {
                          hasNextPage
                          endCursor
                        }
                        edges {
                          cursor
                          node {
                            id
                            title
                            handle
                            description
                            status
                            totalInventory
                            createdAt
                            updatedAt
                            images(first: 1) {
                              edges {
                                node {
                                  url
                                }
                              }
                            }
                            priceRange {
                              minVariantPrice {
                                amount
                                currencyCode
                              }
                              maxVariantPrice {
                                amount
                                currencyCode
                              }
                            }
                            variants(first: 5) {
                              edges {
                                node {
                                  id
                                  title
                                  price
                                  compareAtPrice
                                  sku
                                  inventoryQuantity
                                }
                              }
                            }
                            collections(first: 10) {
                              edges {
                                node {
                                  id
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                    """;
                
                Map<String, Object> variables = new HashMap<>();
                variables.put("first", pageSize);
                if (cursor != null) {
                    variables.put("after", cursor);
                }
                
                JsonNode data = graphQLService.execute(gql, variables);
                JsonNode productsNode = data.get("products");
                JsonNode edges = productsNode.get("edges");
                
                // 收集当前页的商品
                if (edges != null && edges.isArray()) {
                    for (JsonNode edge : edges) {
                        allProducts.add(edge);
                    }
                }
                
                // 检查是否有下一页
                JsonNode pageInfo = productsNode.get("pageInfo");
                boolean hasNextPage = pageInfo.get("hasNextPage").asBoolean();
                
                if (hasNextPage && pageCount < maxPages - 1) {
                    cursor = pageInfo.get("endCursor").asText();
                    pageCount++;
                } else {
                    cursor = null; // 退出循环
                }
                
            } while (cursor != null);
            
            // 4. 获取活跃的折扣规则并关联到商品
            try {
                String rulesJson = getActivePriceRules();
                JsonNode rulesNode = objectMapper.readTree(rulesJson);
                
                // 遍历每个商品，添加适用的折扣
                for (JsonNode edge : allProducts) {
                    JsonNode product = edge.get("node");
                    if (product instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                        List<String> applicableDiscounts = new ArrayList<>();
                        String productId = product.get("id").asText();
                        
                        // 获取商品所属的集合ID列表
                        List<String> productCollectionIds = new ArrayList<>();
                        if (product.has("collections") && product.get("collections").has("edges")) {
                            for (JsonNode cEdge : product.get("collections").get("edges")) {
                                productCollectionIds.add(cEdge.get("node").get("id").asText());
                            }
                        }
                        
                        // 检查每个折扣规则是否适用于当前商品
                        if (rulesNode.has("edges")) {
                            for (JsonNode ruleEdge : rulesNode.get("edges")) {
                                JsonNode discountNode = ruleEdge.get("node");
                                JsonNode discount = discountNode.get("discount");
                                
                                // 获取折扣标题或代码
                                String title = discount.has("title") ? discount.get("title").asText() : "";
                                if (discount.has("codes") && discount.get("codes").has("nodes") && 
                                    discount.get("codes").get("nodes").isArray() && 
                                    discount.get("codes").get("nodes").size() > 0) {
                                    // 使用实际的折扣代码
                                    title = discount.get("codes").get("nodes").get(0).get("code").asText();
                                }
                                
                                boolean applies = false;
                                
                                if (discount.has("customerGets") && discount.get("customerGets").has("items")) {
                                    JsonNode items = discount.get("customerGets").get("items");
                                    
                                    // 检查是否直接应用于该商品
                                    if (items.has("products") && items.get("products").has("edges")) {
                                        for (JsonNode pEdge : items.get("products").get("edges")) {
                                            if (productId.equals(pEdge.get("node").get("id").asText())) {
                                                applies = true;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    // 检查是否应用于商品变体（间接应用于商品）
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
                                    
                                    // 检查是否应用于商品所属的集合
                                    if (!applies && items.has("collections") && items.get("collections").has("edges")) {
                                        for (JsonNode cEdge : items.get("collections").get("edges")) {
                                            if (productCollectionIds.contains(cEdge.get("node").get("id").asText())) {
                                                applies = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                if (applies && !title.isEmpty()) {
                                    applicableDiscounts.add(title);
                                }
                            }
                        }
                        
                        // 将适用的折扣添加到商品节点
                        com.fasterxml.jackson.databind.node.ArrayNode discountsArray = objectMapper.createArrayNode();
                        applicableDiscounts.forEach(discountsArray::add);
                        ((com.fasterxml.jackson.databind.node.ObjectNode) product).set("applicableDiscounts", discountsArray);

                        // 简化变体 ID (变体 ID 不要用这种全名：gid://shopify/ProductVariant/44185938460787 ，直接显示 ID 就行)
                        if (product.has("variants") && product.get("variants").has("edges")) {
                            for (JsonNode vEdge : product.get("variants").get("edges")) {
                                JsonNode variant = vEdge.get("node");
                                if (variant instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                                    String vId = variant.get("id").asText();
                                    if (vId.startsWith("gid://shopify/ProductVariant/")) {
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) variant).put("id", vId.substring(vId.lastIndexOf('/') + 1));
                                    }
                                }
                            }
                        }
                    }
                }
                
                log.info("已为所有商品关联折扣信息: tenantId={}, 商品数量={}", tenantId, allProducts.size());
            } catch (Exception e) {
                log.error("关联折扣信息失败，将返回不带折扣的商品列表: tenantId={}, error={}", 
                        tenantId, e.getMessage(), e);
                // 如果折扣关联失败，仍然返回商品列表，只是没有折扣信息
            }
            
            // 5. 构建响应对象
            Map<String, Object> response = new HashMap<>();
            response.put("edges", allProducts);
            response.put("totalCount", allProducts.size());
            response.put("cached", false);
            response.put("cacheExpiresIn", CACHE_TTL.getSeconds() + " seconds");


            String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            // 6. 存入缓存
            redisTemplate.opsForValue().set(cacheKey, resultJson, CACHE_TTL);

            log.info("已缓存所有商品数据（含折扣）: tenantId={}, 商品数量={}, 过期时间={}分钟", 
                    tenantId, allProducts.size(), CACHE_TTL.toMinutes());


            return resultJson;
            
        } catch (Exception e) {
            log.error("获取所有商品失败", e);
            return "{\"error\": \"Failed to get all products: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 获取当前租户ID
     * 用于缓存键的多租户隔离
     */
    @Tool("Get exchangeable variants for a given variant ID (must have same price)")
    public String getExchangeableVariants(@P(value = "The Shopify Variant ID", required = true) String variantId) {
        log.info("查询可更换变体: variantId={}", variantId);
        
        // 1. 获取当前变体所属的产品ID和价格
        String getVariantGql = """
            query ($id: ID!) {
              productVariant(id: $id) {
                price
                product {
                  id
                  featuredImage {
                    url
                  }
                  variants(first: 50) {
                    edges {
                      node {
                        id
                        title
                        price
                        image {
                          url
                        }
                        selectedOptions {
                          name
                          value
                        }
                        product {
                          title
                          featuredImage {
                            url
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;
            
        try {
            // 确保 ID 格式正确
            String variantGid = variantId.startsWith("gid://") ? variantId : "gid://shopify/ProductVariant/" + variantId;
            
            JsonNode data = graphQLService.execute(getVariantGql, Map.of("id", variantGid));
            JsonNode variantNode = data.get("productVariant");
            
            if (variantNode == null || variantNode.isNull()) {
                log.warn("未找到变体: variantGid={}", variantGid);
                return "Variant not found / 变体不存在";
            }
            
            String currentPrice = variantNode.get("price").asText();
            JsonNode allVariants = variantNode.get("product").get("variants").get("edges");
            
            List<JsonNode> exchangeableVariants = new ArrayList<>();
            for (JsonNode edge : allVariants) {
                JsonNode node = edge.get("node");
                String nodePrice = node.get("price").asText();
                String nodeId = node.get("id").asText();
                
                // 价格一致且不是同一个变体
                if (currentPrice.equals(nodePrice) && !variantGid.equals(nodeId)) {
                    // 简化变体 ID
                    if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                        String vId = node.get("id").asText();
                        if (vId.startsWith("gid://shopify/ProductVariant/")) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("id", vId.substring(vId.lastIndexOf('/') + 1));
                        }
                    }
                    exchangeableVariants.add(node);
                }
            }
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exchangeableVariants);
            
        } catch (Exception e) {
            log.error("查询可更换变体失败: variantId={}", variantId, e);
            return "Failed to get exchangeable variants / 查询可更换变体失败: " + e.getMessage();
        }
    }

    private String getCurrentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("TenantContext中没有租户ID，使用默认值");
            return "default";
        }
        return tenantId;
    }

    /**
     * Search products with pagination support.
     * Not exposed as an AI tool directly to keep the tool definition simple,
     * but used by the REST API controller.
     */
    public String searchProductsWithPagination(String query, int first, String after) {
        String gql = """
            query ($query: String!, $first: Int!, $after: String) {
              products(first: $first, after: $after, query: $query, sortKey: RELEVANCE) {
                pageInfo {
                  hasNextPage
                  endCursor
                }
                edges {
                  cursor
                  node {
                    id
                    title
                    handle
                    description
                    status
                    totalInventory
                    images(first: 1) {
                      edges {
                        node {
                          url
                        }
                      }
                    }
                    priceRange {
                      minVariantPrice {
                        amount
                        currencyCode
                      }
                      maxVariantPrice {
                        amount
                        currencyCode
                      }
                    }
                    variants(first: 1) {
                      edges {
                        node {
                          id
                          price
                          compareAtPrice
                          sku
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("first", first);
        if (after != null && !after.isBlank()) {
            variables.put("after", after);
        }

        JsonNode data = graphQLService.execute(gql, variables);
        JsonNode products = data.get("products");
        
        // 简化变体 ID
        if (products.has("edges") && products.get("edges").isArray()) {
            for (JsonNode edge : products.get("edges")) {
                JsonNode product = edge.get("node");
                if (product.has("variants") && product.get("variants").has("edges")) {
                    for (JsonNode vEdge : product.get("variants").get("edges")) {
                        JsonNode variant = vEdge.get("node");
                        if (variant instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                            String vId = variant.get("id").asText();
                            if (vId.startsWith("gid://shopify/ProductVariant/")) {
                                ((com.fasterxml.jackson.databind.node.ObjectNode) variant).put("id", vId.substring(vId.lastIndexOf('/') + 1));
                            }
                        }
                    }
                }
            }
        }
        
        // We return the 'products' node directly so the caller can access pageInfo and edges
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(products);
        } catch (Exception e) {
            return products.toString();
        }
    }

    @Tool("Get product details and inventory by Product ID (numeric ID)")
    public String getProductDetails(@P(value = "The Shopify Product numeric ID (e.g., 123456789)", required = true) String productId) {
        // 自动补全为 GraphQL ID 格式
        String gid = productId;
        if (!productId.startsWith("gid://")) {
            gid = "gid://shopify/Product/" + productId;
        }

        String gql = """
            query ($id: ID!) {
              product(id: $id) {
                id
                title
                description
                status
                totalInventory
                variants(first: 10) {
                  edges {
                    node {
                      id
                      title
                      sku
                      price
                      inventoryQuantity
                    }
                  }
                }
              }
            }
            """;

        JsonNode data = graphQLService.execute(gql, Map.of("id", gid));
        JsonNode product = data.get("product");
        
        // 简化变体 ID
        if (product.has("variants") && product.get("variants").has("edges")) {
            for (JsonNode vEdge : product.get("variants").get("edges")) {
                JsonNode variant = vEdge.get("node");
                if (variant instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                    String vId = variant.get("id").asText();
                    if (vId.startsWith("gid://shopify/ProductVariant/")) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) variant).put("id", vId.substring(vId.lastIndexOf('/') + 1));
                    }
                }
            }
        }
        
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(product);
        } catch (Exception e) {
            return product.toString();
        }
    }

    @Tool("Get tracking information for an order's fulfillment")
    public String getOrderFulfillmentTracking(@P(value = "The Shopify Order numeric ID", required = true) String orderId) {
        // 自动补全为 GraphQL ID 格式
        String gid = orderId;
        if (!orderId.startsWith("gid://")) {
            gid = "gid://shopify/Order/" + orderId;
        }

        String gql = """
            query ($id: ID!) {
              order(id: $id) {
                id
                name
                displayFulfillmentStatus
                fulfillments(first: 20) {
                  id
                  status
                  trackingInfo(first: 5) {
                    company
                    number
                    url
                  }
                  fulfillmentLineItems(first: 20) {
                    edges {
                      node {
                        lineItem {
                          title
                          quantity
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        JsonNode data = graphQLService.execute(gql, Map.of("id", gid));
        if (data.get("order") == null || data.get("order").isNull()) {
             return "Order not found";
        }
        return data.get("order").toString();
    }

    @Tool("Update the note for a specific order")
    public String updateOrderNote(
            @P(value = "The Shopify Order numeric ID", required = true) String orderId,
            @P(value = "The new note content", required = true) String note) {
        
        String gid = orderId;
        if (!orderId.startsWith("gid://")) {
            gid = "gid://shopify/Order/" + orderId;
        }

        String gql = """
            mutation orderUpdate($input: OrderInput!) {
              orderUpdate(input: $input) {
                order {
                  id
                  note
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;

        Map<String, Object> input = Map.of(
            "id", gid,
            "note", note
        );

        JsonNode data = graphQLService.execute(gql, Map.of("input", input));
        return data.get("orderUpdate").toString();
    }

    @Tool("Update the shipping address for a specific order")
    public String updateShippingAddress(
            @P(value = "The Shopify Order numeric ID", required = true) String orderId,
            @P(value = "Address Line 1", required = true) String address1,
            @P(value = "City", required = true) String city,
            @P(value = "Province/State", required = true) String province,
            @P(value = "Country", required = true) String country,
            @P(value = "Zip/Postal Code", required = true) String zip) {
        
        String gid = orderId;
        if (!orderId.startsWith("gid://")) {
            gid = "gid://shopify/Order/" + orderId;
        }

        String gql = """
            mutation orderUpdate($input: OrderInput!) {
              orderUpdate(input: $input) {
                order {
                  id
                  shippingAddress {
                    address1
                    city
                    province
                    country
                    zip
                  }
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;

        Map<String, Object> address = Map.of(
            "address1", address1,
            "city", city,
            "province", province,
            "country", country,
            "zip", zip
        );

        Map<String, Object> input = Map.of(
            "id", gid,
            "shippingAddress", address
        );

        JsonNode data = graphQLService.execute(gql, Map.of("input", input));
        return data.get("orderUpdate").toString();
    }

    @Tool("Get shop policies (refund policy, shipping policy, privacy policy)")
    public String getShopPolicies() {
        try {
            JsonNode policies = graphQLService.getPolicies();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policies);
        } catch (Exception e) {
            return "Failed to get shop policies: " + e.getMessage();
        }
    }

    @Tool("Get detailed customer information including total spent and order count")
    public String getCustomerDetail(@P(value = "Customer email address", required = true) String email) {
        String gql = """
            query ($query: String!) {
              customers(first: 1, query: $query) {
                edges {
                  node {
                    id
                    firstName
                    lastName
                    email
                    phone
                    totalSpent
                    numberOfOrders
                    tags
                    createdAt
                    lastOrder {
                      createdAt
                      totalPriceSet {
                        shopMoney {
                          amount
                          currencyCode
                        }
                      }
                    }
                  }
                }
              }
            }
            """;
        
        // Search by email
        String query = "email:" + email;
        JsonNode data = graphQLService.execute(gql, Map.of("query", query));
        
        JsonNode edges = data.get("customers").get("edges");
        if (edges.isEmpty()) {
            return "Customer not found";
        }
        
        return edges.get(0).get("node").toString();
    }



    @Tool("Create a discount code (percentage or fixed amount)")
    public String createDiscountCode(
            @P(value = "Discount code (e.g., 'SAVE20')", required = true) String code,
            @P(value = "Discount type ('PERCENTAGE' or 'FIXED_AMOUNT')", required = true) String type,
            @P(value = "Value (e.g., '20' for 20% or $20)", required = true) String value) {
        
        String gql = """
            mutation discountCodeBasicCreate($basicCodeDiscount: DiscountCodeBasicInput!) {
              discountCodeBasicCreate(basicCodeDiscount: $basicCodeDiscount) {
                codeDiscountNode {
                  codeDiscount {
                    ... on DiscountCodeBasic {
                      title
                      codes(first: 1) {
                        nodes {
                          code
                        }
                      }
                      startsAt
                      endsAt
                    }
                  }
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;

        Map<String, Object> customerSelection = Map.of("all", true);
        
        Map<String, Object> customerGets = new java.util.HashMap<>();
        customerGets.put("items", Map.of("all", true));
        
        if ("PERCENTAGE".equalsIgnoreCase(type)) {
            customerGets.put("value", Map.of("percentage", Double.parseDouble(value) / 100.0));
        } else {
            customerGets.put("value", Map.of("discountAmount", Map.of("amount", value, "appliesOnEachItem", false)));
        }

        Map<String, Object> input = Map.of(
            "title", code,
            "code", code,
            "startsAt", java.time.Instant.now().toString(),
            "customerSelection", customerSelection,
            "customerGets", customerGets
        );

        JsonNode data = graphQLService.execute(gql, Map.of("basicCodeDiscount", input));
        JsonNode result = data.get("discountCodeBasicCreate");
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }

    @Tool("Get product recommendations based on a product ID")
    public String getProductRecommendations(@P(value = "The Shopify Product numeric ID", required = true) String productId) {
        String gid = productId;
        if (!productId.startsWith("gid://")) {
            gid = "gid://shopify/Product/" + productId;
        }

        String gql = """
            query ($productId: ID!) {
              productRecommendations(productId: $productId) {
                id
                title
                handle
                priceRange {
                  minVariantPrice {
                    amount
                    currencyCode
                  }
                }
                featuredImage {
                  url
                }
                variants(first: 1) {
                  edges {
                    node {
                      id
                    }
                  }
                }
              }
            }
            """;

        JsonNode data = graphQLService.execute(gql, Map.of("productId", gid));
        JsonNode result = data.get("productRecommendations");
        
        List<Map<String, Object>> cards = new ArrayList<>();
        
        if (result.isArray()) {
            int count = 0;
            for (JsonNode node : result) {
                if (count >= 4) break; // Max 4 items
                
                Map<String, Object> cardData = new HashMap<>();
                
                // ID
                String id = node.path("id").asText();
                if (id.startsWith("gid://shopify/Product/")) {
                    id = id.substring(id.lastIndexOf('/') + 1);
                }
                cardData.put("id", id);
                
                // Title & Handle
                cardData.put("title", node.path("title").asText());
                cardData.put("handle", node.path("handle").asText());
                
                // Price & Currency
                JsonNode priceNode = node.path("priceRange").path("minVariantPrice");
                cardData.put("price", priceNode.path("amount").asText());
                cardData.put("currency", priceNode.path("currencyCode").asText());
                
                // Image
                cardData.put("image", node.path("featuredImage").path("url").asText());
                
                // Variant ID (from first variant)
                JsonNode variants = node.path("variants").path("edges");
                if (variants.isArray() && variants.size() > 0) {
                    String variantId = variants.get(0).path("node").path("id").asText();
                    if (variantId.startsWith("gid://shopify/ProductVariant/")) {
                        variantId = variantId.substring(variantId.lastIndexOf('/') + 1);
                    }
                    cardData.put("variantId", variantId);
                }
                
                cards.add(cardData);
                count++;
            }
        }
        
        if (cards.isEmpty()) {
            return "No recommendations found.";
        }
        
        try {
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cards);
            return "card#CARD_PRODUCT#" + payload;
        } catch (Exception e) {
            return "Failed to process recommendations: " + e.getMessage();
        }
    }

    public JsonNode getProductsOrVariantsDetails(java.util.List<String> ids) {
        String gql = """
            query ($ids: [ID!]!) {
              nodes(ids: $ids) {
                ... on Product {
                  __typename
                  id
                  title
                  handle
                  description
                  totalInventory
                  status
                  featuredImage {
                    url
                  }
                  priceRange {
                    minVariantPrice {
                      amount
                      currencyCode
                    }
                  }
                  collections(first: 10) {
                    edges {
                      node {
                        id
                      }
                    }
                  }
                  variants(first: 1) {
                    edges {
                      node {
                        id
                        price
                        compareAtPrice
                      }
                    }
                  }
                }
                ... on ProductVariant {
                  __typename
                  id
                  title
                  price
                  compareAtPrice
                  sku
                  inventoryQuantity
                  image {
                    url
                  }
                  product {
                    id
                    title
                    handle
                    description
                    status
                    featuredImage {
                      url
                    }
                    priceRange {
                        minVariantPrice {
                            currencyCode
                        }
                    }
                    collections(first: 10) {
                        edges {
                          node {
                            id
                          }
                        }
                    }
                  }
                }
              }
            }
            """;
        
        JsonNode data = graphQLService.execute(gql, Map.of("ids", ids));
        return data.get("nodes");
    }

    public String getActivePriceRules() {
        String gql = """
            query {
              discountNodes(first: 20, query: "status:active", sortKey: CREATED_AT, reverse: true) {
                edges {
                  node {
                    id
                    discount {
                      ... on DiscountCodeBasic {
                        title
                        codes(first: 1) {
                          nodes {
                            code
                          }
                        }
                        customerGets {
                          value {
                            ... on DiscountPercentage {
                              percentage
                            }
                            ... on DiscountAmount {
                              amount {
                                amount
                                currencyCode
                              }
                            }
                          }
                          items {
                            ... on DiscountProducts {
                              products(first: 10) {
                                edges {
                                  node {
                                    id
                                  }
                                }
                              }
                              productVariants(first: 20) {
                                edges {
                                  node {
                                    id
                                    product {
                                      id
                                    }
                                  }
                                }
                              }
                            }
                            ... on DiscountCollections {
                              collections(first: 10) {
                                edges {
                                  node {
                                    id
                                  }
                                }
                              }
                            }
                          }
                        }
                        startsAt
                        endsAt
                      }
                      ... on DiscountAutomaticBasic {
                         title
                         startsAt
                         endsAt
                         customerGets {
                          value {
                            ... on DiscountPercentage {
                              percentage
                            }
                            ... on DiscountAmount {
                              amount {
                                amount
                                currencyCode
                              }
                            }
                          }
                          items {
                            ... on DiscountProducts {
                              products(first: 10) {
                                edges {
                                  node {
                                    id
                                  }
                                }
                              }
                              productVariants(first: 20) {
                                edges {
                                  node {
                                    id
                                    product {
                                      id
                                    }
                                  }
                                }
                              }
                            }
                            ... on DiscountCollections {
                              collections(first: 10) {
                                edges {
                                  node {
                                    id
                                  }
                                }
                              }
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
        JsonNode data = graphQLService.execute(gql, Map.of());
        return data.get("discountNodes").toString();
    }



    private String generateGiftCardCode() {
        // Generate a 16-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String createGiftCard(String amount, String note) {
        String code = generateGiftCardCode();
        
        String gql = """
            mutation giftCardCreate($input: GiftCardCreateInput!) {
              giftCardCreate(input: $input) {
                giftCard {
                  id
                  balance {
                    amount
                    currencyCode
                  }
                  note
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;
        
        Map<String, Object> input = new HashMap<>();
        // Ensure amount is formatted as a string with 2 decimal places (e.g. "10.00")
        String formattedAmount = new java.math.BigDecimal(amount).setScale(2, java.math.RoundingMode.HALF_UP).toString();
        // The API expects initialValue to be a Money scalar (Decimal string), NOT a MoneyInput object
        input.put("initialValue", formattedAmount); 
        input.put("code", code);
        if (note != null && !note.isEmpty()) {
            input.put("note", note);
        }

        JsonNode data = graphQLService.execute(gql, Map.of("input", input));
        JsonNode result = data.get("giftCardCreate");
        
        // Inject the generated code into the result so the frontend can display it
        if (result.has("giftCard") && !result.get("giftCard").isNull()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) result.get("giftCard")).put("code", code);
        }
        
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }

    @Tool("Send email verification code to a customer's email address")
    public String sendEmailVerificationCode(
            @P(value = "Email address to send verification code to", required = true) String email,
            @P(value = "Customer ID (optional, can be null)", required = false) String customerId) {
        
        log.info("发送邮箱验证码: email={}, customerId={}", email, customerId);
        
        try {
            UUID customerUuid = null;
            if (customerId != null && !customerId.isEmpty() && !customerId.equals("null")) {
                try {
                    customerUuid = UUID.fromString(customerId);
                } catch (IllegalArgumentException e) {
                    log.warn("无效的客户ID格式: {}", customerId);
                }
            }
            
            String result = emailVerificationService.sendVerificationCode(email, customerUuid);
            return result;
            
        } catch (Exception e) {
            log.error("发送邮箱验证码失败: email={}", email, e);
            return "Failed to send verification code / 验证码发送失败: " + e.getMessage();
        }
    }

    @Tool("Verify email verification code and bind email to customer")
    public String verifyAndBindEmail(
            @P(value = "Email address", required = true) String email,
            @P(value = "Verification code", required = true) String code,
            @P(value = "Customer ID", required = true) String customerId) {
        
        log.info("验证并绑定邮箱: email={}, customerId={}", email, customerId);
        
        try {
            // 验证必填参数
            if (email == null || email.isBlank()) {
                return "Email is required / 邮箱地址不能为空";
            }
            if (code == null || code.isBlank()) {
                return "Verification code is required / 验证码不能为空";
            }
            if (customerId == null || customerId.isEmpty() || customerId.equals("null")) {
                return "Customer ID is required / 客户ID不能为空";
            }
            
            // 解析客户ID
            UUID customerUuid;
            try {
                customerUuid = UUID.fromString(customerId);
            } catch (IllegalArgumentException e) {
                return "Invalid customer ID format / 客户ID格式无效";
            }
            
            // 调用服务验证并绑定
            String result = emailVerificationService.verifyAndBindEmail(email, code, customerUuid);
            return result;
            
        } catch (Exception e) {
            log.error("验证并绑定邮箱失败: email={}, customerId={}", email, customerId, e);
            return "Failed to verify and bind email / 验证并绑定邮箱失败: " + e.getMessage();
        }
    }

    @Tool("Modify Shopify order (shipping address, variant, phone, email, note). Only for unfulfilled orders.")
    public String modifyOrder(
            @P(value = "Customer ID (UUID format)", required = true) String customerId,
            @P(value = "Shopify Order Number (e.g., '#1001' or '1001')", required = true) String orderNumber,
            @P(value = "New shipping address line 1 (Street name, house number, community name, etc.) / 街道地址：路名、门牌号、小区名", required = false) String newAddress1,
            @P(value = "New shipping address line 2 (Apartment, suite, unit, etc.) / 详细地址：公寓号、套房号等", required = false) String newAddress2,
            @P(value = "New city (optional, null to skip)", required = false) String newCity,
            @P(value = "New province/state (optional, null to skip)", required = false) String newProvince,
            @P(value = "New country (optional, null to skip)", required = false) String newCountry,
            @P(value = "New zip/postal code (optional, null to skip)", required = false) String newZip,
            @P(value = "New phone number (optional, null to skip)", required = false) String newPhone,
            @P(value = "New email (optional, null to skip)", required = false) String newEmail,
            @P(value = "New note/remark (optional, null to skip)", required = false) String newNote,
            @P(value = "The product variant ID(s) that need to be replaced (optional, comma-separated for multiple)", required = false) String oldVariantIds,
            @P(value = "New variant ID(s) to replace with (optional, comma-separated for multiple, must match order of oldVariantIds)", required = false) String newVariantIds) {
        
        log.info("修改订单: customerId={}, orderNumber={}", customerId, orderNumber);
        
        try {
            // 1. 验证客户ID
            if (customerId == null || customerId.isEmpty() || customerId.equals("null")) {
                return "Customer ID is required";
            }
            
            UUID customerUuid;
            try {
                customerUuid = UUID.fromString(customerId);
            } catch (IllegalArgumentException e) {
                return "Invalid customer ID format";
            }
            
            // 2. 查找客户并验证邮箱绑定
            Optional<Customer> customerOpt = customerRepository.findById(customerUuid);
            if (customerOpt.isEmpty()) {
                return "Customer not found";
            }
            
            Customer customer = customerOpt.get();
            String customerEmail = customer.getEmail();
            
            if (customerEmail == null || customerEmail.isBlank()) {
                return "Customer email not bound. Please bind email first";
            }
            
            // 3. 通过订单号查询订单
            String cleanOrderNumber = orderNumber.trim();
            if (!cleanOrderNumber.startsWith("#")) {
                cleanOrderNumber = "#" + cleanOrderNumber;
            }
            
            String searchQuery = String.format("name:%s AND email:%s", cleanOrderNumber, customerEmail);
            
            String searchOrderGql = """
                query ($query: String!) {
                  orders(first: 1, query: $query) {
                    edges {
                      node {
                        id
                        name
                        email
                        phone
                        displayFulfillmentStatus
                        cancelledAt
                        cancelReason
                        shippingAddress {
                          address1
                          address2
                          city
                          province
                          country
                          zip
                          phone
                        }
                        fulfillments {
                          status
                        }
                        lineItems(first: 50) {
                          edges {
                            node {
                              id
                              title
                              quantity
                              variant {
                                id
                                price
                                inventoryQuantity
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;
            
            JsonNode searchResult = graphQLService.execute(searchOrderGql, Map.of("query", searchQuery));
            JsonNode edges = searchResult.path("orders").path("edges");
            
            if (edges.isEmpty() || edges.size() == 0) {
                return String.format("Order %s not found for customer email %s / 未找到订单号 %s 或邮箱不匹配", 
                    cleanOrderNumber, customerEmail, cleanOrderNumber);
            }
            
            JsonNode order = edges.get(0).path("node");
            String gid = order.path("id").asText();
            String orderName = order.path("name").asText();
            
            // 4. 邮箱已在搜索时验证，跳过此步骤
            
            // 5. 检查订单是否已取消
            JsonNode cancelledAtNode = order.get("cancelledAt");
            if (cancelledAtNode != null && !cancelledAtNode.isNull() && !cancelledAtNode.asText().isEmpty()) {
                return "Order has been cancelled and cannot be modified";
            }
            
            // 6. 检查订单配送状态（必须是未配送）
            String fulfillmentStatus = order.get("displayFulfillmentStatus").asText();
            JsonNode fulfillments = order.get("fulfillments");
            
            boolean isFulfilled = false;
            if (fulfillments != null && fulfillments.isArray() && fulfillments.size() > 0) {
                for (JsonNode fulfillment : fulfillments) {
                    String status = fulfillment.get("status").asText();
                    if ("SUCCESS".equalsIgnoreCase(status) || "FULFILLED".equalsIgnoreCase(status)) {
                        isFulfilled = true;
                        break;
                    }
                }
            }
            
            if (isFulfilled || "FULFILLED".equalsIgnoreCase(fulfillmentStatus)) {
                return "Order has been fulfilled and cannot be modified / 订单已发货，无法修改";
            }
            
            // 7. 处理商品变体修改（如果指定）
            boolean variantModified = false;
            if (isNotEmpty(oldVariantIds) && isNotEmpty(newVariantIds)) {
                String[] oldIds = oldVariantIds.split(",");
                String[] newIds = newVariantIds.split(",");
                
                if (oldIds.length != newIds.length) {
                    return "The number of old variant IDs must match the number of new variant IDs";
                }
                
                List<String[]> variantPairs = new ArrayList<>();
                for (int i = 0; i < oldIds.length; i++) {
                    variantPairs.add(new String[]{oldIds[i].trim(), newIds[i].trim()});
                }
                
                String variantCheckResult = checkAndReplaceVariants(order, variantPairs);
                if (!variantCheckResult.equals("OK")) {
                    return variantCheckResult;
                }
                variantModified = true;
            }
            
            // 8. 构建订单更新输入
            Map<String, Object> updateInput = new HashMap<>();
            updateInput.put("id", gid);
            
            // 获取现有配送地址，确保更新时保留未修改的字段
            JsonNode existingAddressNode = order.path("shippingAddress");
            Map<String, Object> address = new HashMap<>();
            
            if (existingAddressNode != null && !existingAddressNode.isMissingNode() && !existingAddressNode.isNull()) {
                 if (existingAddressNode.has("address1")) address.put("address1", existingAddressNode.path("address1").asText());
                 if (existingAddressNode.has("address2")) address.put("address2", existingAddressNode.path("address2").asText());
                 if (existingAddressNode.has("city")) address.put("city", existingAddressNode.path("city").asText());
                 if (existingAddressNode.has("province")) address.put("province", existingAddressNode.path("province").asText());
                 if (existingAddressNode.has("country")) address.put("country", existingAddressNode.path("country").asText());
                 if (existingAddressNode.has("zip")) address.put("zip", existingAddressNode.path("zip").asText());
                 if (existingAddressNode.has("phone")) address.put("phone", existingAddressNode.path("phone").asText());
            }

            // 更新配送地址和电话
            boolean addressModified = false;
            if (isNotEmpty(newAddress1)) { address.put("address1", newAddress1); addressModified = true; }
            if (isNotEmpty(newAddress2)) { address.put("address2", newAddress2); addressModified = true; }
            if (isNotEmpty(newCity)) { address.put("city", newCity); addressModified = true; }
            if (isNotEmpty(newProvince)) { address.put("province", newProvince); addressModified = true; }
            if (isNotEmpty(newCountry)) { address.put("country", newCountry); addressModified = true; }
            if (isNotEmpty(newZip)) { address.put("zip", newZip); addressModified = true; }
            if (isNotEmpty(newPhone)) { address.put("phone", newPhone); addressModified = true; }
            
            if (addressModified && !address.isEmpty()) {
                updateInput.put("shippingAddress", address);
            }
            
            // 更新邮箱
            if (isNotEmpty(newEmail)) {
                updateInput.put("email", newEmail);
            }
            
            // 更新备注
            if (isNotEmpty(newNote)) {
                updateInput.put("note", newNote);
            }
            
            // 9. 执行订单更新
            if (updateInput.size() <= 1) {
                if (variantModified) {
                    log.info("订单变体修改成功（无其他属性修改）: orderNumber={}, customerId={}", orderNumber, customerId);
                    return "Order modification successful:" + orderName;
                }
                return "No changes specified";
            }
            
            String updateGql = """
                mutation orderUpdate($input: OrderInput!) {
                  orderUpdate(input: $input) {
                    order {
                      id
                      name
                    }
                    userErrors {
                      field
                      message
                    }
                  }
                }
                """;
            
            JsonNode updateResult = graphQLService.execute(updateGql, Map.of("input", updateInput));
            JsonNode updateData = updateResult.get("orderUpdate");
            
            // 检查错误
            JsonNode userErrors = updateData.get("userErrors");
            if (userErrors != null && userErrors.isArray() && userErrors.size() > 0) {
                StringBuilder errors = new StringBuilder("Update failed / 更新失败: ");
                for (JsonNode error : userErrors) {
                    errors.append(error.get("message").asText()).append("; ");
                }
                return errors.toString();
            }
            
            log.info("订单修改成功: orderNumber={}, customerId={}", orderNumber, customerId);
            return "Order modification successful:" + orderName;
            
        } catch (Exception e) {
            log.error("修改订单失败: orderNumber={}, customerId={}", orderNumber, customerId, e);
            return "Modification failed: System error. You can try again later:"+orderNumber    ;
        }
    }
    
    /**
     * 检查并替换多个商品变体
     * 验证库存和价格，并执行批量替换操作
     */
    private String checkAndReplaceVariants(JsonNode order, List<String[]> variantPairs) {
        try {
            String orderGid = order.get("id").asText();
            JsonNode lineItems = order.get("lineItems").get("edges");
            
            // 1. 预验证：检查所有变体是否存在、库存和价格
            List<Map<String, Object>> replacements = new ArrayList<>();
            
            for (String[] pair : variantPairs) {
                String oldVariantId = pair[0];
                String newVariantId = pair[1];
                
                String oldVariantGid = oldVariantId.startsWith("gid://") ? oldVariantId : "gid://shopify/ProductVariant/" + oldVariantId;
                String newVariantGid = newVariantId.startsWith("gid://") ? newVariantId : "gid://shopify/ProductVariant/" + newVariantId;
                
                // 查找原始 line item
                JsonNode originalLineItem = null;
                String originalPrice = null;
                int quantity = 0;
                
                for (JsonNode edge : lineItems) {
                    JsonNode node = edge.get("node");
                    if (node.has("variant") && !node.get("variant").isNull() && 
                        node.get("variant").get("id").asText().equals(oldVariantGid) &&
                        node.get("quantity").asInt(0) > 0) {
                        originalLineItem = node;
                        originalPrice = node.get("variant").get("price").asText();
                        quantity = node.get("quantity").asInt();
                        break;
                    }
                }
                
                if (originalLineItem == null) {
                    return "Original variant " + oldVariantId + " not found in order / 订单中未找到原变体 " + oldVariantId;
                }
                
                // 获取新变体信息
                String variantGql = """
                    query ($id: ID!) {
                      productVariant(id: $id) {
                        id
                        price
                        inventoryQuantity
                      }
                    }
                    """;
                JsonNode variantData = graphQLService.execute(variantGql, Map.of("id", newVariantGid));
                JsonNode newVariant = variantData.get("productVariant");
                
                if (newVariant == null || newVariant.isNull()) {
                    return "New variant " + newVariantId + " not found / 新变体 " + newVariantId + " 不存在";
                }
                
                // 检查库存
                int availableInventory = newVariant.get("inventoryQuantity").asInt();
                if (availableInventory < quantity) {
                    return String.format(
                        "Insufficient inventory for variant %s. Required: %d, Available: %d / 变体 %s 库存不足。需要: %d, 可用: %d",
                        newVariantId, quantity, availableInventory, newVariantId, quantity, availableInventory
                    );
                }
                
                // 检查价格
                String newPrice = newVariant.get("price").asText();
                if (originalPrice != null && !originalPrice.equals(newPrice)) {
                    return String.format(
                        "Price mismatch for variant %s. Original: %s, New: %s / 变体 %s 价格不一致。原价格: %s, 新价格: %s",
                        newVariantId, originalPrice, newPrice, newVariantId, originalPrice, newPrice
                    );
                }
                
                Map<String, Object> replacement = new HashMap<>();
                replacement.put("oldVariantGid", oldVariantGid);
                replacement.put("newVariantGid", newVariantGid);
                replacement.put("quantity", quantity);
                replacements.add(replacement);
            }
            
            // 2. 执行订单编辑 (单个会话中处理所有变体)
            log.info("开始批量替换订单变体: order={}, pairs={}", orderGid, variantPairs.size());
            
            // 2.1 开始编辑
            String editBeginGql = """
                mutation orderEditBegin($id: ID!) {
                  orderEditBegin(id: $id) {
                    calculatedOrder {
                      id
                      lineItems(first: 50) {
                        edges {
                          node {
                            id
                            variant {
                              id
                            }
                          }
                        }
                      }
                    }
                    userErrors {
                      field
                      message
                    }
                  }
                }
                """;
            JsonNode beginResult = graphQLService.execute(editBeginGql, Map.of("id", orderGid));
            JsonNode beginData = beginResult.get("orderEditBegin");
            if (beginData.get("userErrors").size() > 0) {
                return "Order edit failed (Begin) : " + beginData.get("userErrors").get(0).get("message").asText();
            }
            
            JsonNode calculatedOrder = beginData.get("calculatedOrder");
            String calculatedOrderId = calculatedOrder.get("id").asText();
            
            // 2.2 循环处理替换
            for (Map<String, Object> replacement : replacements) {
                String oldVariantGid = (String) replacement.get("oldVariantGid");
                String newVariantGid = (String) replacement.get("newVariantGid");
                int quantity = (int) replacement.get("quantity");
                
                // 查找 CalculatedLineItem ID
                String calculatedLineItemId = null;
                JsonNode calcLineItems = calculatedOrder.get("lineItems").get("edges");
                for (JsonNode edge : calcLineItems) {
                    JsonNode node = edge.get("node");
                    if (node.has("variant") && !node.get("variant").isNull() && 
                        node.get("variant").get("id").asText().equals(oldVariantGid)) {
                        calculatedLineItemId = node.get("id").asText();
                        break;
                    }
                }
                
                if (calculatedLineItemId == null) {
                    return "Original variant " + oldVariantGid + " not found in calculated order";
                }
                
                // 移除旧变体
                String removeGql = """
                    mutation orderEditSetQuantity($id: ID!, $lineItemId: ID!, $quantity: Int!) {
                      orderEditSetQuantity(id: $id, lineItemId: $lineItemId, quantity: $quantity) {
                        calculatedOrder {
                          id
                        }
                        userErrors {
                          field
                          message
                        }
                      }
                    }
                    """;
                JsonNode removeResult = graphQLService.execute(removeGql, Map.of("id", calculatedOrderId, "lineItemId", calculatedLineItemId, "quantity", 0));
                if (removeResult.get("orderEditSetQuantity").get("userErrors").size() > 0) {
                    return "Order edit failed (Remove " + oldVariantGid + "): " + removeResult.get("orderEditSetQuantity").get("userErrors").get(0).get("message").asText();
                }
                
                // 添加新变体
                String addGql = """
                    mutation orderEditAddVariant($id: ID!, $variantId: ID!, $quantity: Int!) {
                      orderEditAddVariant(id: $id, variantId: $variantId, quantity: $quantity) {
                        calculatedOrder {
                          id
                        }
                        userErrors {
                          field
                          message
                        }
                      }
                    }
                    """;
                JsonNode addResult = graphQLService.execute(addGql, Map.of("id", calculatedOrderId, "variantId", newVariantGid, "quantity", quantity));
                if (addResult.get("orderEditAddVariant").get("userErrors").size() > 0) {
                    return "Order edit failed (Add " + newVariantGid + "): " + addResult.get("orderEditAddVariant").get("userErrors").get(0).get("message").asText();
                }
            }
            
            // 2.3 提交更改
            String commitGql = """
                mutation orderEditCommit($id: ID!) {
                  orderEditCommit(id: $id) {
                    order {
                      id
                    }
                    userErrors {
                      field
                      message
                    }
                  }
                }
                """;
            JsonNode commitResult = graphQLService.execute(commitGql, Map.of("id", calculatedOrderId));
            if (commitResult.get("orderEditCommit").get("userErrors").size() > 0) {
                return "Order edit failed (Commit) / 订单编辑失败(提交): " + commitResult.get("orderEditCommit").get("userErrors").get(0).get("message").asText();
            }
            
            log.info("批量替换变体成功: order={}", orderGid);
            return "OK";
            
        } catch (Exception e) {
            log.error("批量替换变体失败", e);
            return "Error during variant replacement: " + e.getMessage();
        }
    }
    
    /**
     * 检查字符串是否非空
     */
    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty() && !str.equals("null");
    }

    @Tool("Check order cancellation penalty by order number. Returns whether can cancel for free, with penalty, or not allowed, and the reason based on cancellation policy.")
    public String checkOrderCancellationPenalty(@P(value = "Shopify Order Number (e.g., '#1001' or '1001')", required = true) String orderNumber) {
        
        log.info("检查订单取消惩罚: orderNumber={}", orderNumber);
        
        try {
            // 1. 处理订单号格式：自动添加 # 前缀（如果没有）
            String searchOrderNumber = orderNumber.trim();
            if (!searchOrderNumber.startsWith("#")) {
                searchOrderNumber = "#" + searchOrderNumber;
            }
            
            // 2. 通过订单号查询订单
            String orderGql = """
                query ($query: String!) {
                  orders(first: 1, query: $query) {
                    edges {
                      node {
                        id
                        name
                        createdAt
                        displayFulfillmentStatus
                        fulfillments {
                          status
                        }
                        totalPriceSet {
                          shopMoney {
                            amount
                            currencyCode
                          }
                        }
                      }
                    }
                  }
                }
                """;
            
            String query = "name:" + searchOrderNumber;
            JsonNode data = graphQLService.execute(orderGql, Map.of("query", query));
            JsonNode edges = data.get("orders").get("edges");
            
            // 3. 检查是否找到订单
            if (edges == null || !edges.isArray() || edges.size() == 0) {
                log.info("未找到订单: orderNumber={}", searchOrderNumber);
                return String.format("Order not found: %s", searchOrderNumber);
            }
            
            JsonNode order = edges.get(0).path("node");
            String orderName = order.get("name").asText();
            String createdAtStr = order.get("createdAt").asText();
            Instant orderCreatedAt = Instant.parse(createdAtStr);
            
            // 4. 检查订单配送状态
            String fulfillmentStatus = order.get("displayFulfillmentStatus").asText();
            JsonNode fulfillments = order.get("fulfillments");
            
            boolean isFulfilled = false;
            if (fulfillments != null && fulfillments.isArray() && fulfillments.size() > 0) {
                for (JsonNode fulfillment : fulfillments) {
                    String status = fulfillment.get("status").asText();
                    if ("SUCCESS".equalsIgnoreCase(status) || "FULFILLED".equalsIgnoreCase(status)) {
                        isFulfilled = true;
                        break;
                    }
                }
            }
            
            if (isFulfilled || "FULFILLED".equalsIgnoreCase(fulfillmentStatus)) {
                return String.format("Order %s has been fulfilled and cannot be cancelled", orderName);
            }
            
            // 5. 获取订单金额
            String totalAmountStr = order.get("totalPriceSet").get("shopMoney").get("amount").asText();
            BigDecimal totalAmount = new BigDecimal(totalAmountStr);
            String currencyCode = order.get("totalPriceSet").get("shopMoney").get("currencyCode").asText();
            
            // 6. 使用阶梯匹配逻辑验证取消政策
            OrderCancellationPolicyService.CancellationCheckResult checkResult = 
                cancellationPolicyService.checkCancellationWithLadder(orderCreatedAt);
            
            // 7. 构建详细的返回信息
            StringBuilder result = new StringBuilder();
            result.append("Order Number: ").append(orderName).append("\n");
            result.append("Order Total: ").append(totalAmount).append(" ").append(currencyCode).append("\n");
            result.append("Created At: ").append(createdAtStr).append("\n\n");
            
            // 计算订单已存在的小时数
            long orderAgeHours = java.time.temporal.ChronoUnit.HOURS.between(orderCreatedAt, Instant.now());
            result.append("Order Age: ").append(orderAgeHours).append(" hours\n\n");
            
            if (!checkResult.canCancel()) {
                // 不允许取消
                result.append("Cancellation Status: ❌ NOT ALLOWED\n\n");
                result.append("Reason: ").append(checkResult.message()).append("\n");
                
                if (checkResult.matchedPolicy() != null) {
                    OrderCancellationPolicy policy = checkResult.matchedPolicy();
                    result.append("\nMatched Policy: ").append(policy.getName()).append("\n");
                    if (policy.getCancellableHours() != null) {
                        result.append("Policy Time Limit: ").append(policy.getCancellableHours()).append(" hours\n");
                        long exceededHours = orderAgeHours - policy.getCancellableHours();
                        result.append("Exceeded Time: ").append(exceededHours).append(" hours");
                    }
                }
            } else if (checkResult.penaltyPercentage() != null && checkResult.penaltyPercentage().compareTo(BigDecimal.ZERO) > 0) {
                // 允许取消但有罚金
                BigDecimal penaltyAmount = totalAmount.multiply(checkResult.penaltyPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal refundAmount = totalAmount.subtract(penaltyAmount);
                
                result.append("Cancellation Status: ⚠️ ALLOWED WITH PENALTY\n\n");
                result.append("Reason: ").append(checkResult.message()).append("\n\n");
                result.append("Penalty Percentage: ").append(checkResult.penaltyPercentage()).append("%\n");
                result.append("Penalty Amount: ").append(penaltyAmount).append(" ").append(currencyCode).append("\n");
                result.append("Refund Amount: ").append(refundAmount).append(" ").append(currencyCode).append("\n");
                
                if (checkResult.matchedPolicy() != null) {
                    OrderCancellationPolicy policy = checkResult.matchedPolicy();
                    result.append("\nMatched Policy: ").append(policy.getName()).append("\n");
                    if (policy.getCancellableHours() != null) {
                        result.append("Policy Time Limit: ").append(policy.getCancellableHours()).append(" hours\n");
                        long exceededHours = orderAgeHours - policy.getCancellableHours();
                        result.append("Exceeded Time: ").append(exceededHours).append(" hours");
                    }
                }
            } else {
                // 免费取消
                result.append("Cancellation Status: ✅ FREE CANCELLATION\n\n");
                result.append("Reason: ").append(checkResult.message()).append("\n");
                result.append("Refund Amount: ").append(totalAmount).append(" ").append(currencyCode);
                
                if (checkResult.matchedPolicy() != null) {
                    OrderCancellationPolicy policy = checkResult.matchedPolicy();
                    result.append("\n\nMatched Policy: ").append(policy.getName());
                    if (policy.getCancellableHours() != null) {
                        result.append("\nPolicy Time Limit: ").append(policy.getCancellableHours()).append(" hours");
                    }
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("检查订单取消惩罚失败: orderNumber={}", orderNumber, e);
            return "Check failed: " + e.getMessage();
        }
    }

    @Tool("Cancel Shopify order with cancellation policy validation. Only for unfulfilled orders.")
    public String cancelOrder(
            @P(value = "Customer ID (UUID format)", required = true) String customerId,
            @P(value = "Shopify Order Number (e.g., '#1001' or '1001')", required = true) String orderNumber,
            @P(value = "Cancellation reason (optional)", required = false) String reason) {
        
        log.info("取消订单: customerId={}, orderNumber={}", customerId, orderNumber);
        
        try {
            // 1. 验证客户ID
            if (customerId == null || customerId.isEmpty() || customerId.equals("null")) {
                return "Customer ID is required / 客户ID不能为空";
            }
            
            UUID customerUuid;
            try {
                customerUuid = UUID.fromString(customerId);
            } catch (IllegalArgumentException e) {
                return "Invalid customer ID format / 客户ID格式无效";
            }
            
            // 2. 查找客户并验证邮箱绑定
            Optional<Customer> customerOpt = customerRepository.findById(customerUuid);
            if (customerOpt.isEmpty()) {
                return "Customer not found / 客户不存在";
            }
            
            Customer customer = customerOpt.get();
            String customerEmail = customer.getEmail();
            
            if (customerEmail == null || customerEmail.isBlank()) {
                return "Customer email not bound. Please bind email first / 客户邮箱未绑定，请先绑定邮箱";
            }
            
            // 3. 通过订单号查询订单
            String searchOrderNumber = orderNumber.trim();
            if (!searchOrderNumber.startsWith("#")) {
                searchOrderNumber = "#" + searchOrderNumber;
            }
            
            String orderQueryGql = """
                query ($query: String!) {
                  orders(first: 1, query: $query) {
                    edges {
                      node {
                        id
                        name
                        email
                        createdAt
                        displayFinancialStatus
                        displayFulfillmentStatus
                        cancelledAt
                        cancelReason
                        fulfillments {
                          status
                        }
                        totalPriceSet {
                          shopMoney {
                            amount
                            currencyCode
                          }
                        }
                        lineItems(first: 50) {
                          edges {
                            node {
                              id
                              title
                              quantity
                              originalTotalSet {
                                shopMoney {
                                  amount
                                  currencyCode
                                }
                              }
                            }
                          }
                        }
                        transactions {
                          id
                          kind
                          status
                          gateway
                          amountSet {
                            shopMoney {
                              amount
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;
            
            JsonNode searchResult = graphQLService.execute(orderQueryGql, Map.of("query", "name:" + searchOrderNumber));
            JsonNode edges = searchResult.get("orders").get("edges");
            
            if (edges == null || !edges.isArray() || edges.size() == 0) {
                 return "Order not found: " + searchOrderNumber;
            }

            JsonNode order = edges.get(0).get("node");
            String gid = order.get("id").asText();
            
            // 4. 验证订单邮箱与客户邮箱一致
            String orderEmail = order.get("email").asText();
            if (!customerEmail.equalsIgnoreCase(orderEmail)) {
                return String.format(
                    "Order email (%s) does not match customer email (%s) / 订单邮箱不匹配",
                    orderEmail, customerEmail
                );
            }
            
            // 5. 检查订单是否已取消
            JsonNode cancelledAtNode = order.get("cancelledAt");
            if (cancelledAtNode != null && !cancelledAtNode.isNull() && !cancelledAtNode.asText().isEmpty()) {
                return "Order has already been cancelled / 订单已经被取消";
            }
            
            // 6. 检查订单配送状态（必须是未配送）
            String fulfillmentStatus = order.get("displayFulfillmentStatus").asText();
            JsonNode fulfillments = order.get("fulfillments");
            
            boolean isFulfilled = false;
            if (fulfillments != null && fulfillments.isArray() && fulfillments.size() > 0) {
                for (JsonNode fulfillment : fulfillments) {
                    String status = fulfillment.get("status").asText();
                    if ("SUCCESS".equalsIgnoreCase(status) || "FULFILLED".equalsIgnoreCase(status)) {
                        isFulfilled = true;
                        break;
                    }
                }
            }
            
            if (isFulfilled || "FULFILLED".equalsIgnoreCase(fulfillmentStatus)) {
                return "Order has been fulfilled and cannot be cancelled / 订单已发货，无法取消";
            }
            
            // 7. 检查订单创建时间
            String createdAtStr = order.get("createdAt").asText();
            Instant orderCreatedAt = Instant.parse(createdAtStr);
            
            // 8. 使用阶梯匹配逻辑验证是否满足取消政策
            OrderCancellationPolicyService.CancellationCheckResult checkResult = 
                cancellationPolicyService.checkCancellationWithLadder(orderCreatedAt);
            
            if (!checkResult.canCancel()) {
                return checkResult.message();
            }
            
            // 9. 获取订单金额
            String totalAmountStr = order.get("totalPriceSet").get("shopMoney").get("amount").asText();
            BigDecimal totalAmount = new BigDecimal(totalAmountStr);
            String currencyCode = order.get("totalPriceSet").get("shopMoney").get("currencyCode").asText();
            
            // 10. 计算罚金
            BigDecimal penaltyPercentage = checkResult.penaltyPercentage();
            BigDecimal penaltyAmount = BigDecimal.ZERO;
            BigDecimal refundAmount = totalAmount;
            
            if (penaltyPercentage != null && penaltyPercentage.compareTo(BigDecimal.ZERO) > 0) {
                penaltyAmount = totalAmount.multiply(penaltyPercentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                refundAmount = totalAmount.subtract(penaltyAmount);
            }
            
            // 11. 检查支付状态
            String financialStatus = order.get("displayFinancialStatus").asText();
            boolean isPaid = "PAID".equalsIgnoreCase(financialStatus) || 
                           "PARTIALLY_PAID".equalsIgnoreCase(financialStatus);
            boolean isAuthorized = "AUTHORIZED".equalsIgnoreCase(financialStatus);
            
            // 12. 处理不同的支付情况
            String cancellationMessage;
            
            if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 有罚金
                if (isPaid) {
                    // 已付款：部分退款
                    cancellationMessage = String.format(
                        "Order will be cancelled. Penalty: %.2f %s (%.1f%%), Refund: %.2f %s",
                        penaltyAmount, currencyCode, penaltyPercentage, refundAmount, currencyCode
                    );
                } else if (isAuthorized) {
                    // 仅授权：先扣罚金再取消
                    cancellationMessage = String.format(
                        "Order will be cancelled. Penalty of %.2f %s (%.1f%%) will be charged from authorization",
                        penaltyAmount, currencyCode, penaltyPercentage
                    );
                } else {
                    // 未付款：直接取消
                    cancellationMessage = "Order will be cancelled (not paid)";
                }
            } else {
                // 无罚金
                if (isPaid) {
                    cancellationMessage = String.format(
                        "Order will be cancelled with full refund: %.2f %s",
                        totalAmount, currencyCode
                    );
                } else {
                    cancellationMessage = "Order will be cancelled for free";
                }
            }
            
            String finalMessage = cancellationMessage;
            
            // 13. 如果已付款，先执行退款（在取消订单之前）
            if (isPaid) {
                String captureGql = """
                    mutation orderCapture($input: OrderCaptureInput!) {
                      orderCapture(input: $input) {
                        transaction {
                          id
                          status
                          amountSet {
                            shopMoney {
                              amount
                              currencyCode
                            }
                          }
                        }
                        userErrors {
                          field
                          message
                        }
                      }
                    }
                    """;
                
                Map<String, Object> captureInput = new HashMap<>();
                captureInput.put("id", gid);
                captureInput.put("amount", penaltyAmount.toString());
                captureInput.put("currency", currencyCode);
                
                try {
                    JsonNode captureResult = graphQLService.execute(captureGql, Map.of("input", captureInput));
                    JsonNode captureErrors = captureResult.path("orderCapture").path("userErrors");
                    
                    if (captureErrors != null && captureErrors.isArray() && captureErrors.size() > 0) {
                        String errorMsg = captureErrors.get(0).get("message").asText();
                        log.error("捕获罚金失败: {}", captureErrors);
                        return "Failed to capture penalty: " + errorMsg + 
                               "\nPlease manually capture " + penaltyAmount + " " + currencyCode + " before cancelling.";
                    } else {
                        finalMessage += "\n✅ Penalty captured: " + penaltyAmount + " " + currencyCode + " (" + penaltyPercentage + "%)";
                        log.info("授权订单罚金捕获成功: orderNumber={}, penalty={} {}", orderNumber, penaltyAmount, currencyCode);
                    }
                } catch (Exception captureEx) {
                    log.error("捕获授权金额失败", captureEx);
                    return "Failed to capture penalty: " + captureEx.getMessage() + 
                           "\nPlease manually capture " + penaltyAmount + " " + currencyCode + " before cancelling.";
                }
            }
            
            // 14. 如果是授权订单且有罚金，先捕获罚金
            if (isAuthorized && penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 找到授权交易
                String authorizationTransactionId = null;
                JsonNode transactions = order.get("transactions");
                if (transactions != null && transactions.isArray()) {
                    for (JsonNode transaction : transactions) {
                        String kind = transaction.get("kind").asText();
                        String status = transaction.get("status").asText();
                        if ("AUTHORIZATION".equals(kind) && "SUCCESS".equals(status)) {
                            authorizationTransactionId = transaction.get("id").asText();
                            break;
                        }
                    }
                }
                
                if (authorizationTransactionId == null) {
                    log.error("未找到授权交易");
                    return "Failed to find authorization transaction. Cannot capture penalty.";
                }
                
                String captureGql = """
                    mutation orderCapture($input: OrderCaptureInput!) {
                      orderCapture(input: $input) {
                        transaction {
                          id
                          status
                          amountSet {
                            shopMoney {
                              amount
                              currencyCode
                            }
                          }
                        }
                        userErrors {
                          field
                          message
                        }
                      }
                    }
                    """;
                
                Map<String, Object> captureInput = new HashMap<>();
                captureInput.put("id", gid);
                captureInput.put("parentTransactionId", authorizationTransactionId);
                captureInput.put("amount", penaltyAmount.toString());
                captureInput.put("currency", currencyCode);
                
                try {
                    JsonNode captureResult = graphQLService.execute(captureGql, Map.of("input", captureInput));
                    JsonNode captureErrors = captureResult.path("orderCapture").path("userErrors");
                    
                    if (captureErrors != null && captureErrors.isArray() && captureErrors.size() > 0) {
                        String errorMsg = captureErrors.get(0).get("message").asText();
                        log.error("捕获罚金失败: {}", captureErrors);
                        return "Failed to capture penalty: " + errorMsg + 
                               "\nPlease manually capture " + penaltyAmount + " " + currencyCode + " before cancelling.";
                    } else {
                        finalMessage += "\n✅ Penalty captured: " + penaltyAmount + " " + currencyCode + " (" + penaltyPercentage + "%)";
                        log.info("授权订单罚金捕获成功: orderNumber={}, penalty={} {}", orderNumber, penaltyAmount, currencyCode);
                    }
                } catch (Exception captureEx) {
                    log.error("捕获授权金额失败", captureEx);
                    return "Failed to capture penalty: " + captureEx.getMessage() + 
                           "\nPlease manually capture " + penaltyAmount + " " + currencyCode + " before cancelling.";
                }
            }
            
            // 15. 执行订单取消
            String cancelGql = """
                mutation orderCancel($orderId: ID!, $reason: OrderCancelReason!, $notifyCustomer: Boolean!, $refund: Boolean!, $restock: Boolean!) {
                  orderCancel(
                    orderId: $orderId
                    reason: $reason
                    notifyCustomer: $notifyCustomer
                    refund: $refund
                    restock: $restock
                  ) {
                    job {
                      id
                      done
                    }
                    orderCancelUserErrors {
                      code
                      field
                      message
                    }
                  }
                }
                """;
            
            Map<String, Object> cancelVariables = new HashMap<>();
            cancelVariables.put("orderId", gid);
            cancelVariables.put("reason", "CUSTOMER");
            cancelVariables.put("notifyCustomer", true);
            cancelVariables.put("refund", false); // 不自动退款，我们手动控制退款金额
            cancelVariables.put("restock", true); // 取消后重新入库
            
            JsonNode cancelResult = graphQLService.execute(cancelGql, cancelVariables);
            JsonNode cancelData = cancelResult.get("orderCancel");
            
            // 检查错误
            JsonNode userErrors = cancelData.get("orderCancelUserErrors");
            if (userErrors != null && userErrors.isArray() && userErrors.size() > 0) {
                StringBuilder errors = new StringBuilder("Cancellation failed: ");
                for (JsonNode error : userErrors) {
                    errors.append(error.get("message").asText()).append("; ");
                }
                return errors.toString();
            }
            
            // 授权订单取消后，剩余授权会自动释放
            if (isAuthorized) {
                if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
                    finalMessage += "\n✅ Remaining authorization voided automatically";
                }
            }
            
            log.info("订单取消成功: orderNumber={}, customerId={}, penalty={}%", 
                    orderNumber, customerId, penaltyPercentage);
            
            return "✅ Order cancelled successfully\n\n" + finalMessage;
            
        } catch (Exception e) {
            log.error("取消订单失败: orderNumber={}, customerId={}", orderNumber, customerId, e);
            return "Failed to cancel order: " + e.getMessage();
        }
    }
}
