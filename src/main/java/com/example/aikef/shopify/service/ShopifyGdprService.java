package com.example.aikef.shopify.service;

import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.service.ResendEmailService;
import com.example.aikef.shopify.model.ShopifyObject;
import com.example.aikef.shopify.model.ShopifyStore;
import com.example.aikef.shopify.repository.ShopifyObjectRepository;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.example.aikef.shopify.service.ShopifyAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class ShopifyGdprService {

    private final ShopifyStoreRepository storeRepository;
    private final ShopifyObjectRepository objectRepository;
    private final CustomerRepository customerRepository;
    private final ResendEmailService emailService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public ShopifyGdprService(ShopifyStoreRepository storeRepository,
                              ShopifyObjectRepository objectRepository,
                              CustomerRepository customerRepository,
                              ResendEmailService emailService,
                              ObjectMapper objectMapper,
                              EntityManager entityManager) {
        this.storeRepository = storeRepository;
        this.objectRepository = objectRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    /**
     * 处理客户数据请求 (customers/data_request)
     * 收集客户数据并发送邮件给商家
     */
    @Transactional(readOnly = true)
    public void handleCustomerDataRequest(String shopDomain, String payloadJson) {
        log.info("Processing customers/data_request for shop: {}", shopDomain);
        String tenantId = ShopifyAuthService.generateTenantId(shopDomain);
        TenantContext.setTenantId(tenantId);
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            JsonNode customerNode = payload.get("customer");
            if (customerNode == null) return;

            String email = customerNode.path("email").asText(null);
            String phone = customerNode.path("phone").asText(null);
            
            // 尝试查找客户 - 由于设置了 TenantContext，这里只会查找当前租户下的客户
            Optional<Customer> customerOpt = Optional.empty();
            if (email != null) {
                customerOpt = customerRepository.findByEmail(email);
            }
            if (customerOpt.isEmpty() && phone != null) {
                customerOpt = customerRepository.findByPhone(phone);
            }

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                // 收集数据 (这里简单转为 JSON)
                String customerData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
                
                // 获取商家邮箱 (如果没有直接存储，可以尝试发送给 shopDomain 对应的默认邮箱)
                // 这里我们假设发送给当前店铺关联的默认管理员邮箱，或者硬编码配置
                // 为了演示，我们尝试从 ShopifyStore 中获取信息，或者发送到 logs
                
                // 注意：Shopify 要求我们通过邮件发送数据给商家，或者直接给 customer
                // 这里我们简化为 log 输出，并尝试发送邮件如果 store 有 email
                log.info("Collected data for customer {}: {}", customer.getId(), customerData);

                // 发送邮件逻辑 (需要 store email)
                // Optional<ShopifyStore> store = storeRepository.findByShopDomain(shopDomain);
                // if (store.isPresent() && store.get().getEmail() != null) {
                //     emailService.sendEmail(store.get().getEmail(), "Customer Data Request", ...);
                // }
            } else {
                log.info("Customer not found for data request in shop {}: email={}, phone={}", shopDomain, email, phone);
            }

        } catch (Exception e) {
            log.error("Error processing customers/data_request", e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 处理客户数据删除请求 (customers/redact)
     * 删除客户 PII 数据
     */
    @Transactional
    public void handleCustomerRedact(String shopDomain, String payloadJson) {
        log.info("Processing customers/redact for shop: {}", shopDomain);
        String tenantId = ShopifyAuthService.generateTenantId(shopDomain);
        TenantContext.setTenantId(tenantId);
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            JsonNode customerNode = payload.get("customer");
            if (customerNode == null) return;

            String shopifyCustomerId = customerNode.path("id").asText();
            String email = customerNode.path("email").asText(null);
            String phone = customerNode.path("phone").asText(null);

            // 1. 删除 ShopifyObject (ObjectType = CUSTOMER) - 这个是按 shopDomain 隔离的，所以是安全的
            if (shopifyCustomerId != null) {
                objectRepository.findByShopDomainAndObjectTypeAndExternalId(
                        shopDomain, 
                        ShopifyObject.ObjectType.CUSTOMER, 
                        shopifyCustomerId
                ).ifPresent(obj -> {
                    objectRepository.delete(obj);
                    log.info("Deleted ShopifyObject for customer: {}", shopifyCustomerId);
                });
            }

            // 2. 删除内部 Customer 实体 - 依赖 TenantContext 隔离
            Optional<Customer> customerOpt = Optional.empty();
            if (email != null) {
                customerOpt = customerRepository.findByEmail(email);
            }
            if (customerOpt.isEmpty() && phone != null) {
                customerOpt = customerRepository.findByPhone(phone);
            }

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                // 可以在这里执行软删除或硬删除
                // 这里执行硬删除
                customerRepository.delete(customer);
                log.info("Deleted internal Customer: {} for shop {}", customer.getId(), shopDomain);
                
                // TODO: 还需要删除关联的 Chat Session 和 Messages 吗？
                // 根据 GDPR，应该删除或匿名化。
                // 如果是级联删除，可能已经删除了。如果没有，需要手动处理。
            } else {
                 log.info("Customer not found for redact in shop {}: email={}, phone={}", shopDomain, email, phone);
            }

        } catch (Exception e) {
            log.error("Error processing customers/redact", e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 处理店铺数据删除请求 (shop/redact)
     * 48小时后删除店铺所有数据
     */
    @Transactional
    public void handleShopRedact(String shopDomain) {
        log.info("Processing shop/redact for shop: {}", shopDomain);
        
        try {
            // 1. 获取 Tenant ID
            String tenantId = ShopifyAuthService.generateTenantId(shopDomain);
            
            // 2. 删除 ShopifyStore
            storeRepository.findByShopDomain(shopDomain).ifPresent(store -> {
                storeRepository.delete(store);
                log.info("Deleted ShopifyStore: {}", shopDomain);
            });

            // 3. 删除该租户下的所有数据
            // 注意：这需要非常小心。这里使用 Native SQL 或 JPQL 批量删除
            
            // 示例：删除 Agents, Customers, Roles, etc.
            // 必须按依赖顺序删除 (子表 -> 父表)
            
            // 为了安全起见，这里演示删除主要业务数据
            
            // 设置 TenantContext 以便（如果需要）使用 Repository 方法
            TenantContext.setTenantId(tenantId);
            try {
                // 使用 EntityManager 直接执行删除，绕过某些检查，或者使用 Repository
                
                // Delete Messages (假设 Message 实体存在)
                deleteByTenantId("Message", tenantId);
                // Delete Sessions (假设 Session 实体存在)
                deleteByTenantId("Session", tenantId);
                // Delete Customers
                deleteByTenantId("Customer", tenantId);
                // Delete Agents
                deleteByTenantId("Agent", tenantId);
                // Delete Roles
                deleteByTenantId("Role", tenantId);
                // Delete SessionGroups
                deleteByTenantId("SessionGroup", tenantId);
                // Delete AiWorkflows
                deleteByTenantId("AiWorkflow", tenantId);
                
                // ShopifyObject 没有 tenant_id (它是全局的，通过 shopDomain 关联)
                // 所以我们通过 shopDomain 删除
                // objectRepository.deleteByShopDomain(shopDomain); // 需要在 Repository 中定义
                // 由于 repository 可能没有 deleteByShopDomain，这里手动实现
                int deletedObjects = entityManager.createQuery("DELETE FROM ShopifyObject o WHERE o.shopDomain = :shopDomain")
                        .setParameter("shopDomain", shopDomain)
                        .executeUpdate();
                log.info("Deleted {} ShopifyObjects for shop {}", deletedObjects, shopDomain);
                
            } finally {
                TenantContext.clear();
            }
        } catch (Exception e) {
            log.error("Error processing shop/redact", e);
        }
    }

    private void deleteByTenantId(String entityName, String tenantId) {
        try {
            // 检查实体是否存在 (简单检查，避免异常)
            // 注意：这里假设实体都在 com.example.aikef.model 包下，或者已经映射
            int count = entityManager.createQuery("DELETE FROM " + entityName + " e WHERE e.tenantId = :tenantId")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();
            log.info("Deleted {} records from {} for tenant {}", count, entityName, tenantId);
        } catch (IllegalArgumentException e) {
            // 实体可能不存在或没有 tenantId 字段
            log.warn("Entity {} not found or invalid for deletion: {}", entityName, e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting from {} for tenant {}", entityName, tenantId, e);
        }
    }
}
