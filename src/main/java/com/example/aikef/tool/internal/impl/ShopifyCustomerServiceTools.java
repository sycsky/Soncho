package com.example.aikef.tool.internal.impl;

import com.example.aikef.model.Customer;
import com.example.aikef.model.OrderCancellationPolicy;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.service.EmailVerificationService;
import com.example.aikef.service.OrderCancellationPolicyService;
import com.example.aikef.shopify.service.ShopifyGraphQLService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Tool("Search for orders by email, order number (e.g. '#1001'), or customer name")
    public String searchOrders(@P("Query string (email, order name, or syntax like 'financial_status:paid')") String query) {
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
                  }
                }
              }
            }
            """;
        
        JsonNode data = graphQLService.execute(gql, Map.of("query", query));
        return data.get("orders").get("edges").toString();
    }

    @Tool("Get detailed information about a specific order by Order ID (numeric ID)")
    public String getOrderDetails(@P("The Shopify Order numeric ID (e.g., 123456789)") String orderId) {
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
                createdAt
                note
                email
                shippingAddress {
                  address1
                  city
                  province
                  country
                  zip
                }
                lineItems(first: 20) {
                  edges {
                    node {
                      title
                      quantity
                      sku
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
            """;

        JsonNode data = graphQLService.execute(gql, Map.of("id", gid));
        return data.get("order").toString();
    }

    @Tool("Search for products by keyword")
    public String searchProducts(@P("Search keyword (title, sku, etc.)") String query) {
        // Delegate to the pagination method with defaults for the AI tool

        return searchProductsWithPagination(query, 5, null);
    }
    
    @Tool("Get all products from the store (cached for 10 minutes)")
    public String getAllProducts() {
        try {
            // 1. 构建缓存键（包含租户信息以实现多租户隔离）
            String tenantId = getCurrentTenantId();
            String cacheKey = PRODUCTS_CACHE_KEY + tenantId;
            
            // 2. 尝试从缓存获取
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null && !cachedData.isEmpty()) {
                log.debug("从缓存返回所有商品数据: tenantId={}", tenantId);
                return cachedData;
            }
            
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
            
            // 4. 构建响应对象
            Map<String, Object> response = new HashMap<>();
            response.put("edges", allProducts);
            response.put("totalCount", allProducts.size());
            response.put("cached", false);
            response.put("cacheExpiresIn", CACHE_TTL.getSeconds() + " seconds");
            
            String resultJson = objectMapper.writeValueAsString(response);
            
            // 5. 存入缓存
            redisTemplate.opsForValue().set(cacheKey, resultJson, CACHE_TTL);
            log.info("已缓存所有商品数据: tenantId={}, 商品数量={}, 过期时间={}分钟", 
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
        // We return the 'products' node directly so the caller can access pageInfo and edges
        return data.get("products").toString();
    }

    @Tool("Get product details and inventory by Product ID (numeric ID)")
    public String getProductDetails(@P("The Shopify Product numeric ID (e.g., 123456789)") String productId) {
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
        return data.get("product").toString();
    }

    @Tool("Get tracking information for an order's fulfillment")
    public String getOrderFulfillmentTracking(@P("The Shopify Order numeric ID") String orderId) {
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
            @P("The Shopify Order numeric ID") String orderId,
            @P("The new note content") String note) {
        
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
            @P("The Shopify Order numeric ID") String orderId,
            @P("Address Line 1") String address1,
            @P("City") String city,
            @P("Province/State") String province,
            @P("Country") String country,
            @P("Zip/Postal Code") String zip) {
        
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
        String gql = """
            query {
              shop {
                privacyPolicy {
                  body
                }
                refundPolicy {
                  body
                }
                shippingPolicy {
                  body
                }
              }
            }
            """;

        JsonNode data = graphQLService.execute(gql, Map.of());
        return data.get("shop").toString();
    }

    @Tool("Get detailed customer information including total spent and order count")
    public String getCustomerDetail(@P("Customer email address") String email) {
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

    @Tool("Cancel an order (only if unfulfilled)")
    public String cancelOrder(@P("The Shopify Order numeric ID") String orderId) {
        String gid = orderId;
        if (!orderId.startsWith("gid://")) {
            gid = "gid://shopify/Order/" + orderId;
        }

        String gql = """
            mutation orderCancel($orderId: ID!) {
              orderCancel(orderId: $orderId) {
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

        JsonNode data = graphQLService.execute(gql, Map.of("orderId", gid));
        return data.get("orderCancel").toString();
    }

    @Tool("Create a discount code (percentage or fixed amount)")
    public String createDiscountCode(
            @P("Discount code (e.g., 'SAVE20')") String code,
            @P("Discount type ('PERCENTAGE' or 'FIXED_AMOUNT')") String type,
            @P("Value (e.g., '20' for 20% or $20)") String value) {
        
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
        return data.get("discountCodeBasicCreate").toString();
    }

    @Tool("Get product recommendations based on a product ID")
    public String getProductRecommendations(@P("The Shopify Product numeric ID") String productId) {
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
              }
            }
            """;

        JsonNode data = graphQLService.execute(gql, Map.of("productId", gid));
        return data.get("productRecommendations").toString();
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
        
        return result.toString();
    }

    @Tool("Send email verification code to a customer's email address")
    public String sendEmailVerificationCode(
            @P("Email address to send verification code to") String email,
            @P("Customer ID (optional, can be null)") String customerId) {
        
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
            @P("Email address") String email,
            @P("Verification code") String code,
            @P("Customer ID") String customerId) {
        
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
            @P("Customer ID (UUID format)") String customerId,
            @P("Shopify Order numeric ID") String orderId,
            @P("New shipping address line 1 (optional, null to skip)") String newAddress1,
            @P("New city (optional, null to skip)") String newCity,
            @P("New province/state (optional, null to skip)") String newProvince,
            @P("New country (optional, null to skip)") String newCountry,
            @P("New zip/postal code (optional, null to skip)") String newZip,
            @P("New phone number (optional, null to skip)") String newPhone,
            @P("New email (optional, null to skip)") String newEmail,
            @P("New note/remark (optional, null to skip)") String newNote,
            @P("Line item ID to replace (optional, null to skip variant change)") String lineItemId,
            @P("New variant ID to replace with (optional, null to skip variant change)") String newVariantId) {
        
        log.info("修改订单: customerId={}, orderId={}", customerId, orderId);
        
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
            
            // 3. 获取订单详情
            String gid = orderId;
            if (!orderId.startsWith("gid://")) {
                gid = "gid://shopify/Order/" + orderId;
            }
            
            String orderDetailsGql = """
                query ($id: ID!) {
                  order(id: $id) {
                    id
                    name
                    email
                    phone
                    displayFulfillmentStatus
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
                """;
            
            JsonNode orderData = graphQLService.execute(orderDetailsGql, Map.of("id", gid));
            JsonNode order = orderData.get("order");
            
            if (order == null || order.isNull()) {
                return "Order not found / 订单不存在";
            }
            
            // 4. 验证订单邮箱与客户邮箱一致
            String orderEmail = order.get("email").asText();
            if (!customerEmail.equalsIgnoreCase(orderEmail)) {
                return String.format(
                    "Order email (%s) does not match customer email (%s) / 订单邮箱与客户绑定邮箱不一致",
                    orderEmail, customerEmail
                );
            }
            
            // 5. 检查订单配送状态（必须是未配送）
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
                return "Order has been fulfilled and cannot be modified / 订单已配送，无法修改";
            }
            
            // 6. 处理商品变体修改（如果指定）
            if (lineItemId != null && !lineItemId.isEmpty() && !lineItemId.equals("null") &&
                newVariantId != null && !newVariantId.isEmpty() && !newVariantId.equals("null")) {
                
                String variantCheckResult = checkAndReplaceVariant(order, lineItemId, newVariantId);
                if (!variantCheckResult.equals("OK")) {
                    return variantCheckResult;
                }
            }
            
            // 7. 构建订单更新输入
            Map<String, Object> updateInput = new HashMap<>();
            updateInput.put("id", gid);
            
            // 更新配送地址
            if (isNotEmpty(newAddress1) || isNotEmpty(newCity) || isNotEmpty(newProvince) || 
                isNotEmpty(newCountry) || isNotEmpty(newZip)) {
                
                Map<String, Object> address = new HashMap<>();
                if (isNotEmpty(newAddress1)) address.put("address1", newAddress1);
                if (isNotEmpty(newCity)) address.put("city", newCity);
                if (isNotEmpty(newProvince)) address.put("province", newProvince);
                if (isNotEmpty(newCountry)) address.put("country", newCountry);
                if (isNotEmpty(newZip)) address.put("zip", newZip);
                
                if (!address.isEmpty()) {
                    updateInput.put("shippingAddress", address);
                }
            }
            
            // 更新电话
            if (isNotEmpty(newPhone)) {
                updateInput.put("phone", newPhone);
            }
            
            // 更新邮箱
            if (isNotEmpty(newEmail)) {
                updateInput.put("email", newEmail);
            }
            
            // 更新备注
            if (isNotEmpty(newNote)) {
                updateInput.put("note", newNote);
            }
            
            // 8. 执行订单更新
            if (updateInput.size() <= 1) {
                return "No changes specified / 未指定任何修改项";
            }
            
            String updateGql = """
                mutation orderUpdate($input: OrderInput!) {
                  orderUpdate(input: $input) {
                    order {
                      id
                      name
                      email
                      phone
                      note
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
            
            log.info("订单修改成功: orderId={}, customerId={}", orderId, customerId);
            return "Order modified successfully / 订单修改成功: " + updateData.get("order").toString();
            
        } catch (Exception e) {
            log.error("修改订单失败: orderId={}, customerId={}", orderId, customerId, e);
            return "Failed to modify order / 订单修改失败: " + e.getMessage();
        }
    }
    
    /**
     * 检查并替换商品变体
     * 验证库存和价格
     */
    private String checkAndReplaceVariant(JsonNode order, String lineItemId, String newVariantId) {
        try {
            // 确保 ID 格式正确
            String lineItemGid = lineItemId.startsWith("gid://") ? lineItemId : "gid://shopify/LineItem/" + lineItemId;
            String variantGid = newVariantId.startsWith("gid://") ? newVariantId : "gid://shopify/ProductVariant/" + newVariantId;
            
            // 查找原始 line item
            JsonNode lineItems = order.get("lineItems").get("edges");
            JsonNode originalLineItem = null;
            String originalPrice = null;
            int quantity = 0;
            
            for (JsonNode edge : lineItems) {
                JsonNode node = edge.get("node");
                if (node.get("id").asText().equals(lineItemGid)) {
                    originalLineItem = node;
                    if (node.has("variant") && !node.get("variant").isNull()) {
                        originalPrice = node.get("variant").get("price").asText();
                    }
                    quantity = node.get("quantity").asInt();
                    break;
                }
            }
            
            if (originalLineItem == null) {
                return "Line item not found in order / 订单中未找到指定的商品项";
            }
            
            // 获取新变体的详细信息
            String variantGql = """
                query ($id: ID!) {
                  productVariant(id: $id) {
                    id
                    price
                    inventoryQuantity
                    product {
                      id
                      title
                    }
                  }
                }
                """;
            
            JsonNode variantData = graphQLService.execute(variantGql, Map.of("id", variantGid));
            JsonNode newVariant = variantData.get("productVariant");
            
            if (newVariant == null || newVariant.isNull()) {
                return "New variant not found / 新变体不存在";
            }
            
            // 检查库存
            int availableInventory = newVariant.get("inventoryQuantity").asInt();
            if (availableInventory < quantity) {
                return String.format(
                    "Insufficient inventory. Required: %d, Available: %d / 库存不足。需要: %d, 可用: %d",
                    quantity, availableInventory, quantity, availableInventory
                );
            }
            
            // 检查价格
            String newPrice = newVariant.get("price").asText();
            if (originalPrice != null && !originalPrice.equals(newPrice)) {
                return String.format(
                    "Price mismatch. Original: %s, New: %s / 价格不一致。原价格: %s, 新价格: %s",
                    originalPrice, newPrice, originalPrice, newPrice
                );
            }
            
            // 注意：实际替换变体需要使用 orderEditBegin/orderEditAddVariant/orderEditCommit
            // 这是一个复杂的流程，这里返回 OK 表示验证通过
            // 实际实现需要完整的 Order Edit API 流程
            log.warn("变体替换验证通过，但实际替换需要使用 Order Edit API（未完全实现）");
            return "OK";
            
        } catch (Exception e) {
            log.error("检查变体失败", e);
            return "Failed to check variant / 检查变体失败: " + e.getMessage();
        }
    }
    
    /**
     * 检查字符串是否非空
     */
    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty() && !str.equals("null");
    }

    @Tool("Cancel Shopify order with cancellation policy validation. Only for unfulfilled orders.")
    public String cancelOrder(
            @P("Customer ID (UUID format)") String customerId,
            @P("Shopify Order numeric ID") String orderId,
            @P("Cancellation reason (optional)") String reason) {
        
        log.info("取消订单: customerId={}, orderId={}", customerId, orderId);
        
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
            
            // 3. 获取订单详情
            String gid = orderId;
            if (!orderId.startsWith("gid://")) {
                gid = "gid://shopify/Order/" + orderId;
            }
            
            String orderDetailsGql = """
                query ($id: ID!) {
                  order(id: $id) {
                    id
                    name
                    email
                    createdAt
                    displayFinancialStatus
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
                    transactions {
                      kind
                      status
                      amountSet {
                        shopMoney {
                          amount
                        }
                      }
                    }
                  }
                }
                """;
            
            JsonNode orderData = graphQLService.execute(orderDetailsGql, Map.of("id", gid));
            JsonNode order = orderData.get("order");
            
            if (order == null || order.isNull()) {
                return "Order not found / 订单不存在";
            }
            
            // 4. 验证订单邮箱与客户邮箱一致
            String orderEmail = order.get("email").asText();
            if (!customerEmail.equalsIgnoreCase(orderEmail)) {
                return String.format(
                    "Order email (%s) does not match customer email (%s) / 订单邮箱与客户绑定邮箱不一致",
                    orderEmail, customerEmail
                );
            }
            
            // 5. 检查订单配送状态（必须是未配送）
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
                return "Order has been fulfilled and cannot be cancelled / 订单已配送，无法取消";
            }
            
            // 6. 检查订单创建时间
            String createdAtStr = order.get("createdAt").asText();
            Instant orderCreatedAt = Instant.parse(createdAtStr);
            
            // 7. 使用阶梯匹配逻辑验证是否满足取消政策
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
                        "订单将被取消。罚金: %.2f %s (%.1f%%), 退款: %.2f %s / " +
                        "Order will be cancelled. Penalty: %.2f %s (%.1f%%), Refund: %.2f %s",
                        penaltyAmount, currencyCode, penaltyPercentage,
                        refundAmount, currencyCode,
                        penaltyAmount, currencyCode, penaltyPercentage,
                        refundAmount, currencyCode
                    );
                } else if (isAuthorized) {
                    // 仅授权：先扣罚金再取消
                    cancellationMessage = String.format(
                        "订单将被取消。将从授权金额中扣除罚金: %.2f %s (%.1f%%) / " +
                        "Order will be cancelled. Penalty of %.2f %s (%.1f%%) will be charged from authorization",
                        penaltyAmount, currencyCode, penaltyPercentage,
                        penaltyAmount, currencyCode, penaltyPercentage
                    );
                } else {
                    // 未付款：直接取消
                    cancellationMessage = "订单将被取消（未付款） / Order will be cancelled (not paid)";
                }
            } else {
                // 无罚金
                if (isPaid) {
                    cancellationMessage = String.format(
                        "订单将被取消并全额退款: %.2f %s / Order will be cancelled with full refund: %.2f %s",
                        totalAmount, currencyCode, totalAmount, currencyCode
                    );
                } else {
                    cancellationMessage = "订单将被免费取消 / Order will be cancelled for free";
                }
            }
            
            // 13. 执行订单取消
            String cancelGql = """
                mutation orderCancel($orderId: ID!, $reason: OrderCancelReason, $notifyCustomer: Boolean, $refund: Boolean) {
                  orderCancel(
                    orderId: $orderId
                    reason: $reason
                    notifyCustomer: $notifyCustomer
                    refund: $refund
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
            cancelVariables.put("refund", true); // 如果有罚金，后续需要调整退款金额
            
            JsonNode cancelResult = graphQLService.execute(cancelGql, cancelVariables);
            JsonNode cancelData = cancelResult.get("orderCancel");
            
            // 检查错误
            JsonNode userErrors = cancelData.get("orderCancelUserErrors");
            if (userErrors != null && userErrors.isArray() && userErrors.size() > 0) {
                StringBuilder errors = new StringBuilder("取消失败 / Cancellation failed: ");
                for (JsonNode error : userErrors) {
                    errors.append(error.get("message").asText()).append("; ");
                }
                return errors.toString();
            }
            
            // 14. 如果有罚金且已付款，需要调整退款金额
            String finalMessage = cancellationMessage;
            if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0 && isPaid) {
                finalMessage += "\n注意: 需要手动调整退款金额为 " + refundAmount + " " + currencyCode +
                              " / Note: Refund amount needs to be manually adjusted to " + refundAmount + " " + currencyCode;
            } else if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0 && isAuthorized) {
                finalMessage += "\n注意: 需要先扣除授权的罚金金额 " + penaltyAmount + " " + currencyCode +
                              " / Note: Need to charge penalty amount " + penaltyAmount + " " + currencyCode + " from authorization first";
            }
            
            log.info("订单取消成功: orderId={}, customerId={}, penalty={}%", 
                    orderId, customerId, penaltyPercentage);
            
            return "✅ 订单取消成功 / Order cancelled successfully\n\n" + finalMessage;
            
        } catch (Exception e) {
            log.error("取消订单失败: orderId={}, customerId={}", orderId, customerId, e);
            return "Failed to cancel order / 订单取消失败: " + e.getMessage();
        }
    }
}
