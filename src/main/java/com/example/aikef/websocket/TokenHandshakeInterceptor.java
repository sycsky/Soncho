package com.example.aikef.websocket;

import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import com.example.aikef.security.TokenService;
import com.example.aikef.service.CustomerTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenHandshakeInterceptor.class);
    private final TokenService tokenService;
    private final CustomerTokenService customerTokenService;

    public TokenHandshakeInterceptor(TokenService tokenService, 
                                    CustomerTokenService customerTokenService) {
        this.tokenService = tokenService;
        this.customerTokenService = customerTokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.info("🤝 WebSocket 握手请求: URI={}, 请求类型={}, Headers={}", 
                request.getURI(), request.getClass().getSimpleName(), request.getHeaders());
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            String queryString = servletRequest.getServletRequest().getQueryString();
            log.info("📋 请求参数: queryString={}, token={}", queryString, token != null ? "存在(长度:" + token.length() + ")" : "不存在");
            
            if (token != null && !token.isBlank()) {
                // 判断是客户 Token 还是坐席 Token
                if (token.startsWith("cust_")) {
                    // 客户 Token
                    Optional<CustomerPrincipal> principalOpt = customerTokenService.resolve(token);
                    if (principalOpt.isPresent()) {
                        CustomerPrincipal principal = principalOpt.get();
                        attributes.put("CUSTOMER_PRINCIPAL", principal);
                        log.info("客户 WebSocket 认证成功: {}, 渠道: {}", principal.getName(), principal.getChannel());
                        return true;
                    } else {
                        log.warn("客户 WebSocket token 无效或已过期: {}", token);
                        return false;
                    }
                } else {
                    // 坐席 Token
                    Optional<AgentPrincipal> principalOpt = tokenService.resolve(token);
                    if (principalOpt.isPresent()) {
                        AgentPrincipal principal = principalOpt.get();
                        attributes.put("AGENT_PRINCIPAL", principal);
                        log.info("坐席 WebSocket 认证成功: {}", principal.getUsername());
                        return true;
                    } else {
                        log.warn("坐席 WebSocket token 无效或已过期: {}", token);
                        return false;
                    }
                }
            }
            log.warn("WebSocket 连接缺少 token 参数，URI: {}", request.getURI());
            return false;
        }
        
        log.warn("非 ServletServerHttpRequest 类型的请求，允许通过");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Do nothing
    }
}
