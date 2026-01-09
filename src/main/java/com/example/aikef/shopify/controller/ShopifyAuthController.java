package com.example.aikef.shopify.controller;

import com.example.aikef.shopify.service.ShopifyAuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shopify/oauth")
@Slf4j
public class ShopifyAuthController {

    private final ShopifyAuthService authService;

    @Value("${shopify.embedded:true}")
    private boolean embedded;

    @Value("${shopify.api-key:}")
    private String apiKey;

    @Value("${shopify.ui-url:https://son-cho.com}")
    private String uiUrl;

    @Value("${shopify.app-url:https://admin.son-cho.com}")
    private String appUrl;

    public ShopifyAuthController(ShopifyAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/install")
    public ResponseEntity<Void> install(@RequestParam String shop) {
        log.info("install shop: {}", shop);
        authService.validateShopDomain(shop);
        URI redirect = authService.buildInstallRedirect(shop);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirect);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String shop,
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam String hmac,
            @RequestParam(required = false) String host,
            @RequestParam(required = false, name = "format") String format,
            HttpServletRequest request
    ) {
        ShopifyAuthService.OAuthCallbackResult result = authService.handleCallback(
                request.getQueryString(),
                shop,
                code,
                state,
                hmac
        );

        boolean wantsJson = "json".equalsIgnoreCase(format);
        String accept = request.getHeader("Accept");
        if (!wantsJson && accept != null && accept.toLowerCase().contains("application/json")) {
            wantsJson = true;
        }

        if (wantsJson) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("shop", result.shopDomain());
            resp.put("tenantId", result.tenantId());
            if (host != null && !host.isBlank()) {
                resp.put("host", host);
            }
            return ResponseEntity.ok(resp);
        }

        URI redirect;
        if (embedded && apiKey != null && !apiKey.isBlank() && host != null && !host.isBlank()) {
            String qs = "shop=" + urlEncode(result.shopDomain()) + "&host=" + urlEncode(host) + "&embedded=1";
            redirect = URI.create("https://" + result.shopDomain() + "/admin/apps/" + urlEncode(apiKey) + "?" + qs);
        } else {
            String targetBase = (uiUrl != null && !uiUrl.isBlank()) ? uiUrl : appUrl;
            String qs = "shop=" + urlEncode(result.shopDomain()) + "&tenantId=" + urlEncode(result.tenantId());
            if (host != null && !host.isBlank()) {
                qs += "&host=" + urlEncode(host);
            }
            redirect = URI.create(targetBase + "/?" + qs);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirect);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String urlEncode(String v) {
        if (v == null) {
            return "";
        }
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
