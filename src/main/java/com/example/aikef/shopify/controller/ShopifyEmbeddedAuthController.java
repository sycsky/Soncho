package com.example.aikef.shopify.controller;

import com.example.aikef.dto.AgentDto;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Agent;
import com.example.aikef.model.Role;
import com.example.aikef.model.enums.AgentStatus;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.RoleRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.TokenService;
import com.example.aikef.shopify.model.ShopifyStore;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.example.aikef.shopify.service.ShopifyAuthService;
import com.example.aikef.shopify.service.ShopifySessionService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shopify/auth")
public class ShopifyEmbeddedAuthController {

    private final TokenService tokenService;
    private final ShopifyStoreRepository storeRepository;
    private final AgentRepository agentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShopifySessionService shopifySessionService;
    private final EntityMapper entityMapper;

    @org.springframework.beans.factory.annotation.Value("${shopify.scopes:read_price_rules,read_discounts_allocator_functions,read_orders,read_customers,read_products,read_discounts,write_price_rules,write_gift_cards,write_orders,write_order_edits,read_inventory,read_checkouts,read_draft_orders,read_themes,read_shipping}")
    private String requiredScopes;

    public ShopifyEmbeddedAuthController(TokenService tokenService,
                                        ShopifyStoreRepository storeRepository,
                                        AgentRepository agentRepository,
                                        RoleRepository roleRepository,
                                        PasswordEncoder passwordEncoder,
                                        ShopifySessionService shopifySessionService,
                                        EntityMapper entityMapper) {
        this.tokenService = tokenService;
        this.storeRepository = storeRepository;
        this.agentRepository = agentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.shopifySessionService = shopifySessionService;
        this.entityMapper = entityMapper;
    }

    @PostMapping("/exchange")
    public ResponseEntity<Map<String, Object>> exchange(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "shop", required = false) String shop
    ) {
        if (shop != null && !shop.isBlank() && StringUtils.isBlank(authorization)) {
            Optional<ShopifyStore> storeOpt = storeRepository.findByShopDomain(shop);
            if (storeOpt.isEmpty() || !storeOpt.get().isActive()) {
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("success", false, "error", "Shop not installed"));
            }
//            if (!hasAllRequiredScopes(storeOpt.get().getScopes(), requiredScopes)) {
//                return ResponseEntity.status(HttpStatus.OK).body(Map.of("success", false, "error", "Shop not installed"));
//            }
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("success", false, "error", "installed"));
        }

        String token = extractBearer(authorization);

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "error", "Shop not installed"));
        }

        ShopifySessionService.VerifiedSession session = shopifySessionService.verifySessionToken(token, shop);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "Invalid session token"));
        }

        String shopDomain = session.shopDomain();
        String tenantId = ShopifyAuthService.generateTenantId(shopDomain);

        TenantContext.setTenantId(tenantId);
        try {
            Optional<ShopifyStore> storeOpt = storeRepository.findByShopDomain(shopDomain);
            if (storeOpt.isEmpty() || !storeOpt.get().isActive()) {
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("success", false, "error", "Shop not installed"));
            }
            if (!hasAllRequiredScopes(storeOpt.get().getScopes(), requiredScopes)) {
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("success", false, "error", "Shop not installed"));
            }

            Agent agent = ensureShopAdminAgent(shopDomain, tenantId);
            AgentPrincipal principal = new AgentPrincipal(agent, com.example.aikef.security.SecurityUtils.getAuthorities(agent));
            String appToken = tokenService.issueToken(principal);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "shop", shopDomain,
                    "tenantId", tenantId,
                    "token", appToken
            ));
        } finally {
            TenantContext.clear();
        }
    }

    private boolean hasAllRequiredScopes(String storeScopes, String requiredScopes) {
        if (storeScopes == null || storeScopes.isBlank()) {
            return false;
        }
        if (requiredScopes == null || requiredScopes.isBlank()) {
            return true;
        }
        java.util.Set<String> storeSet = java.util.Arrays.stream(storeScopes.split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());

        return java.util.Arrays.stream(requiredScopes.split(","))
                .map(String::trim)
                .allMatch(storeSet::contains);
    }

    @GetMapping("/agents")
    public ResponseEntity<Map<String, Object>> listAgents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "shop", required = false) String shop
    ) {
        String token = extractBearer(authorization);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "error", "Missing session token"));
        }

        ShopifySessionService.VerifiedSession session = shopifySessionService.verifySessionToken(token, shop);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "Invalid session token"));
        }

        String tenantId = ShopifyAuthService.generateTenantId(session.shopDomain());
        TenantContext.setTenantId(tenantId);
        try {
            List<AgentDto> agents = agentRepository.findByTenantId(tenantId).stream()
                    .map(entityMapper::toAgentDto)
                    .toList();
            return ResponseEntity.ok(Map.of("success", true, "agents", agents));
        } finally {
            TenantContext.clear();
        }
    }

    private Agent ensureShopAdminAgent(String shopDomain, String tenantId) {
        String email = "shopify-admin@" + shopDomain;
        Optional<Agent> existing = agentRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get();
        }

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

        String password = "123456";

        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Shopify Admin (" + shopDomain + ")");
        agent.setEmail(email);
        agent.setPasswordHash(passwordEncoder.encode(password));
        agent.setStatus(AgentStatus.ONLINE);
        agent.setRole(adminRole);
        agent.setLanguage("en");
        Agent saved = agentRepository.save(agent);
        

        
        return saved;
    }

    private String extractBearer(String authorization) {
        if (authorization == null) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

}

