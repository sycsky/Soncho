package com.example.aikef.saas.interceptor;

import com.example.aikef.saas.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;

import org.springframework.util.AntPathMatcher;
import java.util.List;
import java.util.Arrays;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Value("${app.saas.enabled:false}")
    private boolean saasEnabled;
    
    // Whitelist paths that do not require tenant context
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/api/health",
            "/api/v1/webhook/**",
            "/api/v1/official-channels/*/webhook",
            "/api/v1/events/hook/**",
            "/api/v1/auth/login",
            "/api/v1/public/**",
            "/api/v1/shopify/auth/exchange",
            "/api/v1/shopify/auth/**",
            "/api/v1/shopify/oauth/**",
            "/api/v1/shopify/webhooks/**",
            "//api/v1/files/upload",
//            "/api/v1/chat/sessions/**",
            "/api/v1/files/image/**",
            "/api/public/cms/**", // CMS Public API
            "/api/admin/cms/**", // CMS Admin API (Handled by manual token check)
            "/actuator/**",
            "/error",
            "/ws/**"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!saasEnabled) {
            return true;
        }

        String requestUri = request.getRequestURI();
        // Check whitelist - Only allow if no tenant ID is present but path is whitelisted
        for (String pattern : WHITELIST_PATHS) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }

        // 1. Try to get Tenant ID from Security Context (Token)
        String tenantId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof AgentPrincipal) {
                AgentPrincipal principal = (AgentPrincipal) authentication.getPrincipal();
                tenantId = principal.getTenantId();
                log.debug("Found Tenant ID from Agent Token: {}", tenantId);
            } else if (authentication.getPrincipal() instanceof CustomerPrincipal) {
                CustomerPrincipal principal = (CustomerPrincipal) authentication.getPrincipal();
                tenantId = principal.getTenantId();
                log.debug("Found Tenant ID from Customer Token: {}", tenantId);
            }
        }

        // 2. Fallback to Header
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader(TENANT_HEADER);
            if (tenantId != null) log.debug("Found Tenant ID from Header: {}", tenantId);
        }
        
        // 3. Fallback to Parameter
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getParameter("tenantId");
            if (tenantId != null) log.debug("Found Tenant ID from Parameter: {}", tenantId);
        }

        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(tenantId);
            return true;
        }



        // If not in whitelist and no tenant ID provided, block request
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Tenant ID (Token, X-Tenant-ID header or tenantId parameter is required)");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
