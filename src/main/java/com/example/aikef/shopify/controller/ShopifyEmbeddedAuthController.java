package com.example.aikef.shopify.controller;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final ObjectMapper objectMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${shopify.api-key:}")
    private String apiKey;

    @Value("${shopify.api-secret:}")
    private String apiSecret;

    public ShopifyEmbeddedAuthController(TokenService tokenService,
                                        ShopifyStoreRepository storeRepository,
                                        AgentRepository agentRepository,
                                        RoleRepository roleRepository,
                                        PasswordEncoder passwordEncoder,
                                        ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.storeRepository = storeRepository;
        this.agentRepository = agentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
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
            }else{
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("success", false, "error", "installed"));
            }
        }

        String token = extractBearer(authorization);

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "error", "Shop not installed"));
        }

        VerifiedSession session;
        try {
            session = verifySessionToken(token, shop);
        } catch (Exception e) {
            session = null;
        }
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

            Agent agent = ensureShopAdminAgent(shopDomain, tenantId);
            AgentPrincipal principal = new AgentPrincipal(
                    agent,
                    agent.getRole() != null
                            ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + agent.getRole().getName().toUpperCase()))
                            : Collections.emptyList()
            );
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
            return roleRepository.save(role);
        });

        byte[] passwordBytes = new byte[24];
        secureRandom.nextBytes(passwordBytes);
        String password = HexFormat.of().formatHex(passwordBytes);

        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Shopify Admin (" + shopDomain + ")");
        agent.setEmail(email);
        agent.setPasswordHash(passwordEncoder.encode(password));
        agent.setStatus(AgentStatus.OFFLINE);
        agent.setRole(adminRole);
        agent.setLanguage("zh-CN");
        return agentRepository.save(agent);
    }

    private VerifiedSession verifySessionToken(String token, String expectedShop) throws Exception {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Missing shopify.api-key or shopify.api-secret");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSig = hmacSha256(apiSecret, signingInput);
        byte[] providedSig = base64UrlDecode(parts[2]);
        if (!constantTimeEquals(expectedSig, providedSig)) {
            return null;
        }

        JsonNode payload = objectMapper.readTree(new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8));

        if (!audMatches(payload.get("aud"), apiKey)) {
            return null;
        }

        long now = Instant.now().getEpochSecond();
        long exp = payload.path("exp").asLong(0);
        if (exp <= 0 || exp <= now) {
            return null;
        }
        long nbf = payload.path("nbf").asLong(0);
        if (nbf > 0 && nbf > now) {
            return null;
        }

        String dest = textOrNull(payload.get("dest"));
        String iss = textOrNull(payload.get("iss"));
        String shopDomain = extractShopDomain(dest != null ? dest : iss);
        if (shopDomain == null || shopDomain.isBlank()) {
            return null;
        }

        if (expectedShop != null && !expectedShop.isBlank() && !expectedShop.equalsIgnoreCase(shopDomain)) {
            return null;
        }

        return new VerifiedSession(shopDomain, exp);
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

    private String extractShopDomain(String urlOrHost) {
        if (urlOrHost == null || urlOrHost.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(urlOrHost);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
        } catch (Exception e) {
        }
        return urlOrHost;
    }

    private boolean audMatches(JsonNode audNode, String apiKey) {
        if (audNode == null || audNode.isNull()) {
            return false;
        }
        if (audNode.isTextual()) {
            return apiKey.equals(audNode.asText());
        }
        if (audNode.isArray()) {
            for (JsonNode v : audNode) {
                if (v != null && v.isTextual() && apiKey.equals(v.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private byte[] hmacSha256(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] base64UrlDecode(String s) {
        String padded = s;
        int mod = padded.length() % 4;
        if (mod != 0) {
            padded += "=".repeat(4 - mod);
        }
        return Base64.getUrlDecoder().decode(padded);
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String s = node.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private record VerifiedSession(String shopDomain, long exp) {}
}

