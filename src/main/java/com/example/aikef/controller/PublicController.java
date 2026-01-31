package com.example.aikef.controller;


import com.example.aikef.dto.request.CreateTenantAdminRequest;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.service.AgentService;
import com.example.aikef.service.RoleService;
import com.example.aikef.dto.AgentDto;
import com.example.aikef.model.Role;
import com.example.aikef.repository.RoleRepository;
import com.example.aikef.shopify.service.ShopifyAuthService;
import jakarta.persistence.EntityNotFoundException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import cn.hutool.json.JSONObject;
import com.example.aikef.dto.CustomerTokenResponse;
import com.example.aikef.dto.request.QuickCustomerRequest;
import com.example.aikef.model.Customer;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import com.example.aikef.security.TokenService;
import com.example.aikef.service.CustomerService;
import com.example.aikef.service.CustomerTokenService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public")
@Slf4j
public class PublicController {

    private final CustomerService customerService;
    private final CustomerTokenService customerTokenService;
    private final TokenService tokenService;
    private final AgentService agentService;
    private final RoleRepository roleRepository;
    private final RedissonClient redissonClient;
    
    @Value("${app.saas.enabled:false}")
    private boolean saasEnabled;

    public PublicController(CustomerService customerService,
                           CustomerTokenService customerTokenService,
                           TokenService tokenService,
                           AgentService agentService,
                           RoleRepository roleRepository,
                           RedissonClient redissonClient) {
        this.customerService = customerService;
        this.customerTokenService = customerTokenService;
        this.tokenService = tokenService;
        this.agentService = agentService;
        this.roleRepository = roleRepository;
        this.redissonClient = redissonClient;
    }

    /**
     * 测试接口：创建租户管理员（仅用于测试，无需认证）
     * 自动分配 tenantId 并创建管理员账号
     */
    @GetMapping("/create-tenant-admin")
    public Map<String, Object> createTenantAdminTest() {
        if (!saasEnabled) {
            throw new RuntimeException("SAAS mode is not enabled");
        }
        
        String tenantId = "tenant_" + System.currentTimeMillis();
        String email = "admin_" + tenantId + "@example.com";
        String password = "password123";
        String name = "Admin " + tenantId;
        
        // 查找或创建 ADMIN 角色，并确保拥有所有权限
        Role adminRole = roleRepository.findByName("Administrator").orElseGet(() -> {
            Role role = new Role();
            role.setName("Administrator");
            role.setDescription("System-wide administrator with all permissions.");
            role.setSystem(true);
            // 设置所有权限为true
            role.setPermissions(com.example.aikef.model.PermissionConstants.createAllPermissionsMap());
            return roleRepository.save(role);
        });
        
        // 如果角色已存在但没有权限，则更新权限
        if (adminRole.getPermissions() == null || adminRole.getPermissions().isEmpty()) {
            adminRole.setPermissions(com.example.aikef.model.PermissionConstants.createAllPermissionsMap());
            adminRole = roleRepository.save(adminRole);
        }
        
        CreateTenantAdminRequest request = new CreateTenantAdminRequest(
                name,
                email,
                password,
                adminRole.getId(),
                "zh",
                tenantId
        );
        
        AgentDto agent = agentService.createAgent(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("email", email);
        result.put("password", password);
        result.put("agentId", agent.id());
        result.put("role", "ADMIN");
        
        return result;
    }


    /**
     * 快速获取客户 Token（用于客户端首次连接）
     * 如果客户不存在则自动创建
     * 同时创建聊天会话和群组，并分配客服
     * 
     * @param request 包含客户信息和可选的 metadata
     *                metadata 字段可包含：
     *                - categoryId: 会话分类ID (String，UUID格式)
     *                - source: 来源渠道
     *                - referrer: 来源页面
     *                - device: 设备信息
     *                - 其他自定义字段...
     */
    @PostMapping("/customer-token")
    public CustomerTokenResponse getCustomerToken(@Valid @RequestBody QuickCustomerRequest request) {
        // 设置租户上下文
        if (request.shop() != null && !request.shop().isBlank()) {
            String tenantId = ShopifyAuthService.generateTenantId(request.shop());
            TenantContext.setTenantId(tenantId);
        }

        RLock lock = null;
        try {
            // 如果是 Shopify 渠道，优先使用 shopifyCustomerId 作为 channelUserId
            String effectiveChannelUserId = request.channelUserId();
            if (request.channel() == com.example.aikef.model.Channel.SHOPIFY && 
                (effectiveChannelUserId == null || effectiveChannelUserId.isBlank())) {
                effectiveChannelUserId = request.shopifyCustomerId();
            }

            // 使用 Redisson 分布式锁，防止并发创建重复用户
            if (effectiveChannelUserId != null && !effectiveChannelUserId.isBlank()) {
                String lockKey = "lock:customer:create:" + request.channel() + ":" + effectiveChannelUserId;
                lock = redissonClient.getLock(lockKey);
                try {
                    // 尝试获取锁，等待 5 秒，持有 10 秒
                    if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                        throw new RuntimeException("系统繁忙，请稍后重试");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("获取锁被中断", e);
                }
            }

            // 查找或创建客户
            Customer customer = customerService.findOrCreateByChannel(
                    request.channel(),
                    request.name(),
                    request.email(),
                    request.phone(),
                    effectiveChannelUserId,
                    request.shopifyCustomerInfo(),
                    request.shopifyCustomerId() != null && !request.shopifyCustomerId().isBlank()
            );

            // 生成 Token 并创建会话（带 metadata）
            CustomerTokenResponse response = customerService.generateCustomerToken(
                    customer.getId(),
                    request.metadata()
            );

            return response;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            TenantContext.clear();
        }
    }

    /**
     * 验证 Token 是否有效
     * 支持客户 Token 和客服 Token
     * 
     * @param token 查询参数中的 token（优先）
     * @param authorization Authorization 请求头（次选）
     * @return 验证结果
     */
    @GetMapping("/validate-token")
    public Map<String, Object> validateToken(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 1. 获取 token（优先使用查询参数，否则从 Authorization 头获取）
        String tokenToValidate = token;
        if (tokenToValidate == null && authorization != null && authorization.startsWith("Bearer ")) {
            tokenToValidate = authorization.substring(7);
        }
        
        // 2. 检查是否提供了 token
        if (tokenToValidate == null || tokenToValidate.isBlank()) {
            result.put("valid", false);
            result.put("error", "MISSING_TOKEN");
            result.put("message", "缺少 token 参数");
            return result;
        }
        
        // 3. 尝试验证客户 Token
        if (tokenToValidate.startsWith("cust_")) {
            return customerTokenService.resolve(tokenToValidate)
                    .map(principal -> {
                        Map<String, Object> validResult = new HashMap<>();
                        validResult.put("valid", true);
                        validResult.put("type", "customer");
                        validResult.put("customerId", principal.getId().toString());
                        validResult.put("name", principal.getName());
                        validResult.put("channel", principal.getChannel());
                        return validResult;
                    })
                    .orElseGet(() -> {
                        Map<String, Object> invalidResult = new HashMap<>();
                        invalidResult.put("valid", false);
                        invalidResult.put("type", "customer");
                        invalidResult.put("error", "TOKEN_EXPIRED");
                        invalidResult.put("message", "客户 Token 无效或已过期");
                        return invalidResult;
                    });
        }
        
        // 4. 尝试验证客服 Token
        return tokenService.resolve(tokenToValidate)
                .map(principal -> {
                    Map<String, Object> validResult = new HashMap<>();
                    validResult.put("valid", true);
                    validResult.put("type", "agent");
                    validResult.put("agentId", principal.getId().toString());
                    validResult.put("username", principal.getUsername());
                    return validResult;
                })
                .orElseGet(() -> {
                    Map<String, Object> invalidResult = new HashMap<>();
                    invalidResult.put("valid", false);
                    invalidResult.put("type", "agent");
                    invalidResult.put("error", "TOKEN_EXPIRED");
                    invalidResult.put("message", "客服 Token 无效或已过期");
                    return invalidResult;
                });
    }


    @PostMapping("/test3")
    public String test3(@RequestBody JSONObject req) {

        log.info("test3 {}", req);



        return "";
    }
}
