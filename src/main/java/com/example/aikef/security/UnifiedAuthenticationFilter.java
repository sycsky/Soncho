package com.example.aikef.security;

import com.example.aikef.service.CustomerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 统一认证过滤器
 * 支持坐席 Token 和客户 Token
 */
@Component
public class UnifiedAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final CustomerTokenService customerTokenService;

    public UnifiedAuthenticationFilter(TokenService tokenService,
                                      CustomerTokenService customerTokenService) {
        this.tokenService = tokenService;
        this.customerTokenService = customerTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            if (token.startsWith("cust_")) {
                // 客户 Token
                customerTokenService.resolve(token).ifPresent(customerPrincipal -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    customerPrincipal,
                                    null,
                                    Collections.emptyList()
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } else {
                // 坐席 Token
                tokenService.resolve(token).ifPresent(agentPrincipal -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    agentPrincipal,
                                    null,
                                    agentPrincipal.getAuthorities()
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
