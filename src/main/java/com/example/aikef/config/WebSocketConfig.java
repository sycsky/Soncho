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
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:3001", 
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:3001"
                )
                .addInterceptors(tokenHandshakeInterceptor)
                .withSockJS();
        
        // 原生 WebSocket 支持
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:3001",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:3001"
                )
                .addInterceptors(tokenHandshakeInterceptor);
    }
}
