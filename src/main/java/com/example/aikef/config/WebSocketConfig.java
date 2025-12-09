package com.example.aikef.config;

import com.example.aikef.websocket.ChatWebSocketHandler;
import com.example.aikef.websocket.TokenHandshakeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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
        // 原生 WebSocket 支持（推荐在 App Runner 上使用，放在前面优先匹配）
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(tokenHandshakeInterceptor);

        // SockJS 备用支持（使用不同的路径避免冲突）
        registry.addHandler(chatWebSocketHandler, "/ws/sockjs/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(tokenHandshakeInterceptor)
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000)
                .setSessionCookieNeeded(false)
                .setWebSocketEnabled(true)
                .setSupressCors(true);  // 禁用SockJS的CORS处理，使用我们自己的
    }

    /**
     * 配置 WebSocket 容器参数
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(120000L);  // 2分钟空闲超时
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }

    /**
     * 心跳任务调度器
     */
    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }
}
