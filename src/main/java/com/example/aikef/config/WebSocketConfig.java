package com.example.aikef.config;

import com.example.aikef.websocket.ChatWebSocketHandler;
import com.example.aikef.websocket.TokenHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final TokenHandshakeInterceptor tokenHandshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                          TokenHandshakeInterceptor tokenHandshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.tokenHandshakeInterceptor = tokenHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 使用 SockJS
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")  // 允许所有源（生产环境）
                .addInterceptors(tokenHandshakeInterceptor)
                .withSockJS();
        
        // 原生 WebSocket 支持
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")  // 允许所有源（生产环境）
                .addInterceptors(tokenHandshakeInterceptor);
    }
}
