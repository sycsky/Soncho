package com.example.aikef.controller;

import com.example.aikef.dto.AgentDto;
import com.example.aikef.dto.request.LoginRequest;
import com.example.aikef.dto.response.LoginResponse;
import com.example.aikef.model.Agent;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.TokenService;
import com.example.aikef.service.AgentService;
import com.example.aikef.shopify.service.ShopifyAuthService;
import com.example.aikef.shopify.service.ShopifySessionService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AgentService agentService;
    private final AgentRepository agentRepository;
    private final ShopifySessionService shopifySessionService;

    public AuthController(AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          AgentService agentService,
                          AgentRepository agentRepository,
                          ShopifySessionService shopifySessionService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.agentService = agentService;
        this.agentRepository = agentRepository;
        this.shopifySessionService = shopifySessionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String email = request.email();
        String tenantId = null;

        if (request.shopifySessionToken() != null && !request.shopifySessionToken().isBlank()) {
            ShopifySessionService.VerifiedSession session = shopifySessionService.verifySessionToken(
                    request.shopifySessionToken(), request.shop());
            if (session == null) {
                throw new IllegalArgumentException("Invalid Shopify session token");
            }
            tenantId = ShopifyAuthService.generateTenantId(session.shopDomain());
            TenantContext.setTenantId(tenantId);
        }

        try {
            if (request.agentId() != null) {
                Agent agent = agentRepository.findById(request.agentId())
                        .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
                if (tenantId != null && agent.getTenantId() != null && !tenantId.equals(agent.getTenantId())) {
                    throw new IllegalArgumentException("Agent does not belong to tenant");
                }
                email = agent.getEmail();
            }

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email or agentId is required");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            AgentPrincipal principal = (AgentPrincipal) authentication.getPrincipal();
            String token = tokenService.issueToken(principal);
            return new LoginResponse(token, agentService.getAgent(principal.getId()));
        } finally {
            if (tenantId != null) {
                TenantContext.clear();
            }
        }
    }

    @GetMapping("/me")
    public AgentDto me() {
        return agentService.currentAgent();
    }
}
