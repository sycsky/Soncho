package com.example.aikef.shopify.service;

import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.model.Agent;
import com.example.aikef.model.Role;
import com.example.aikef.model.enums.AgentStatus;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.RoleRepository;
import com.example.aikef.service.SessionGroupService;
import com.example.aikef.shopify.model.ShopifyStore;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.example.aikef.model.Event;
import com.example.aikef.repository.EventRepository;
import com.example.aikef.model.AiWorkflow;
import com.example.aikef.model.SessionGroup;
import com.example.aikef.repository.AiWorkflowRepository;
import com.example.aikef.repository.SessionGroupRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@Transactional
public class ShopifyAuthService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration TOKEN_CACHE_TTL = Duration.ofDays(30);
    private static final String STATE_KEY_PREFIX = "shopify:oauth:state:";
    private static final String TOKEN_KEY_PREFIX = "shopify:access_token:";

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final ShopifyStoreRepository storeRepository;
    private final ShopifyWebhookRegistrationService webhookRegistrationService;
    private final AgentRepository agentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AiWorkflowRepository aiWorkflowRepository;
    private final EventRepository eventRepository;
    private final EntityManager entityManager;


    @Autowired
    private SessionGroupService sessionGroupService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${shopify.api-key:}")
    private String apiKey;

    @Value("${shopify.api-secret:}")
    private String apiSecret;

    @Value("${shopify.scopes:read_orders,read_customers,read_products,read_discounts,write_price_rules,write_gift_cards,write_orders,write_order_edits,read_inventory,read_checkouts,read_draft_orders,read_themes,read_shipping,read_fulfillments,read_returns}")
    private String scopes;

    @Value("${shopify.app-url:http://localhost:8080}")
    private String appUrl;

    public ShopifyAuthService(StringRedisTemplate redisTemplate,
                             RestTemplate restTemplate,
                             ShopifyStoreRepository storeRepository,
                             ShopifyWebhookRegistrationService webhookRegistrationService,
                             AgentRepository agentRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             AiWorkflowRepository aiWorkflowRepository,
                             EventRepository eventRepository,
                             EntityManager entityManager) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.storeRepository = storeRepository;
        this.webhookRegistrationService = webhookRegistrationService;
        this.agentRepository = agentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.aiWorkflowRepository = aiWorkflowRepository;
        this.eventRepository = eventRepository;
        this.entityManager = entityManager;
    }

    public URI buildInstallRedirect(String shopDomain) {
        String state = generateState();
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, shopDomain, STATE_TTL);

        String redirectUri = appUrl + "/api/v1/shopify/oauth/callback";

        String url = "https://" + shopDomain + "/admin/oauth/authorize"
                + "?client_id=" + urlEncode(apiKey)
                + "&scope=" + urlEncode(scopes)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&state=" + urlEncode(state);

        return URI.create(url);
    }

    public OAuthCallbackResult handleCallback(String queryString, String shop, String code, String state, String hmac) {
        validateShopDomain(shop);
        String stateKey = STATE_KEY_PREFIX + state;
        String expectedShop = redisTemplate.opsForValue().get(stateKey);
        if (expectedShop == null || !expectedShop.equals(shop)) {
            throw new IllegalArgumentException("Invalid state");
        }
        redisTemplate.delete(stateKey);

        if (!verifyHmac(queryString, hmac)) {
            throw new IllegalArgumentException("Invalid hmac");
        }

        TokenResponse tokenResponse = exchangeAccessToken(shop, code);
        String shopEmail = fetchShopEmail(shop, tokenResponse.access_token());
        ShopifyStore store = upsertStore(shop, tokenResponse.access_token(), tokenResponse.scope(), shopEmail);
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + shop, store.getAccessToken(), TOKEN_CACHE_TTL);
        webhookRegistrationService.registerAll(shop, store.getAccessToken());

        return new OAuthCallbackResult(store.getShopDomain(), store.getTenantId());
    }

    public Optional<String> getAccessToken(String shopDomain) {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + shopDomain);
        if (cached != null && !cached.isBlank()) {
            return Optional.of(cached);
        }
        return storeRepository.findByShopDomain(shopDomain)
                .map(ShopifyStore::getAccessToken)
                .filter(v -> v != null && !v.isBlank())
                .map(token -> {
                    redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + shopDomain, token, TOKEN_CACHE_TTL);
                    return token;
                });
    }

    public static String generateTenantId(String shopDomain) {
        if (shopDomain == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(shopDomain.getBytes(StandardCharsets.UTF_8));
            return "shp_" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

    private ShopifyStore upsertStore(String shopDomain, String accessToken, String scope, String shopEmail) {
        String tenantId = generateTenantId(shopDomain);
        TenantContext.setTenantId(tenantId);
        try {
            ShopifyStore store = storeRepository.findByShopDomain(shopDomain).orElseGet(ShopifyStore::new);
            store.setShopDomain(shopDomain);
            store.setAccessToken(accessToken);
            store.setScopes(scope);
            store.setActive(true);
            store.setInstalledAt(Instant.now());
            store.setUninstalledAt(null);
            store.setTenantId(tenantId);
            ShopifyStore saved = storeRepository.save(store);
            Agent adminAgent = ensureShopAdminAgent(shopDomain, tenantId, shopEmail);
            initializeTenantData(adminAgent);
            return saved;
        } finally {
            TenantContext.clear();
        }
    }

    private void initializeTenantData(Agent adminAgent) {
        // 1. 确保默认会话分组存在
        sessionGroupService.ensureDefaultGroups(adminAgent);
    }
    
    @SuppressWarnings("unchecked")
    private void copyTemplateWorkflows(Agent adminAgent) {
        // 使用 EntityManager 执行原生 SQL 查询模板工作流，绕过租户过滤器
        // 注意：这里假设模板工作流的 is_template = true
        // 为了避免复制已经存在的模板，可以先检查
        
        try {
            // 查询所有模板工作流 (忽略租户过滤器)
            String sql = "SELECT * FROM ai_workflows WHERE is_template = true";
            var query = entityManager.createNativeQuery(sql, AiWorkflow.class);
            var templates = (java.util.List<AiWorkflow>) query.getResultList();
            
            for (AiWorkflow template : templates) {
                // 检查当前租户是否已经复制过该模板（通过名称判断，或者允许重复？）
                // 简单起见，如果当前租户下没有同名工作流，则复制
                boolean exists = aiWorkflowRepository.findByName(template.getName()).isPresent();
                if (!exists) {
                    AiWorkflow copy = new AiWorkflow();
                    BeanUtils.copyProperties(template, copy, "id", "createdAt", "updatedAt", "version");
                    
                    // TenantEntityListener 会自动填充 tenantId (前提是 copy.getTenantId() == null)
                    copy.setTenantId(null); 
                    
                    copy.setTemplate(false); // 复制后的不再是模板
                    copy.setEnabled(true);
                    copy.setCreatedByAgent(adminAgent); // 设置创建人为当前的管理员
                    aiWorkflowRepository.save(copy);
                }
            }
        } catch (Exception e) {
            // 记录日志但不阻断流程
            System.err.println("Failed to copy template workflows: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Agent ensureShopAdminAgent(String shopDomain, String tenantId, String shopEmail) {
        String email = (shopEmail != null && !shopEmail.isBlank()) ? shopEmail : "shopify-admin@" + shopDomain;
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
        agent.setName("Shopify Admin");
        agent.setEmail(email);
        agent.setPasswordHash(passwordEncoder.encode(password));
        agent.setStatus(AgentStatus.ONLINE);
        agent.setRole(adminRole);
        agent.setLanguage("zh-CN");
        agent =  agentRepository.save(agent);
        TenantContext.setTenantId(tenantId);
        // 2. 复制模板工作流
        copyTemplateWorkflows(agent);
        // 3. 复制模板事件
        copyTemplateEvents(agent);

        return agent;
    }

    @SuppressWarnings("unchecked")
    private void copyTemplateEvents(Agent adminAgent) {
        try {
            // 查询所有模板事件 (忽略租户过滤器)
            String sql = "SELECT * FROM events WHERE is_template = true";
            var query = entityManager.createNativeQuery(sql, Event.class);
            var templates = (java.util.List<Event>) query.getResultList();
            
            for (Event template : templates) {
                // 检查当前租户是否已经复制过该模板
                boolean exists = eventRepository.findByName(template.getName()).isPresent();
                if (!exists) {
                    Event copy = new Event();
                    BeanUtils.copyProperties(template, copy, "id", "createdAt", "updatedAt", "version");
                    
                    copy.setTenantId(null); // TenantEntityListener handles it
                    copy.setTemplate(false); // 复制后的不再是模板
                    copy.setEnabled(true);
                    
                    eventRepository.save(copy);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to copy template events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private TokenResponse exchangeAccessToken(String shopDomain, String code) {
        String url = "https://" + shopDomain + "/admin/oauth/access_token";
        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_id", apiKey);
        body.put("client_secret", apiSecret);
        body.put("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(url, entity, TokenResponse.class);
        TokenResponse tokenResponse = response.getBody();
        if (tokenResponse == null || tokenResponse.access_token() == null || tokenResponse.access_token().isBlank()) {
            throw new IllegalStateException("Token exchange failed");
        }
        return tokenResponse;
    }

    private boolean verifyHmac(String queryString, String providedHmac) {
        if (providedHmac == null || providedHmac.isBlank()) {
            return false;
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Missing shopify.api-secret");
        }
        String message = canonicalizeQueryString(queryString);
        String expected = hmacSha256Hex(apiSecret, message);
        return constantTimeEquals(expected, providedHmac.toLowerCase());
    }

    private String canonicalizeQueryString(String queryString) {
        if (queryString == null) {
            return "";
        }
        Map<String, String> params = new TreeMap<>();
        for (String pair : queryString.split("&")) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            if ("hmac".equals(key) || "signature".equals(key)) {
                continue;
            }
            params.put(key, value);
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC calculation failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    public void validateShopDomain(String shopDomain) {
        if (shopDomain == null || shopDomain.isBlank()) {
            throw new IllegalArgumentException("Missing shop");
        }
        if (!shopDomain.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-]*\\.myshopify\\.com$")) {
            throw new IllegalArgumentException("Invalid shop");
        }
    }

    public record OAuthCallbackResult(String shopDomain, String tenantId) {}

    public record TokenResponse(String access_token, String scope) {}

    private String fetchShopEmail(String shopDomain, String accessToken) {
        String url = "https://" + shopDomain + "/admin/api/2024-01/shop.json";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<ShopResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, ShopResponse.class);
            if (response.getBody() != null && response.getBody().shop() != null) {
                return response.getBody().shop().email();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch shop email: " + e.getMessage());
        }
        return null;
    }

    private record ShopResponse(ShopDto shop) {}
    private record ShopDto(String email) {}
}
