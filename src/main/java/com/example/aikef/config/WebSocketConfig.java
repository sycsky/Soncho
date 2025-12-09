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
        // 使用 SockJS（针对 App Runner 优化配置）
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(tokenHandshakeInterceptor)
                .withSockJS()
                .setHeartbeatTime(25000)           // 25秒心跳间隔
                .setDisconnectDelay(5000)          // 5秒断开延迟
                .setHttpMessageCacheSize(1000)     // 消息缓存大小
                .setStreamBytesLimit(512 * 1024)   // 流字节限制 512KB
                .setSessionCookieNeeded(false)     // 不需要session cookie
                .setWebSocketEnabled(true)         // 确保启用WebSocket
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
        
        // 原生 WebSocket 支持（推荐在 App Runner 上使用）
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(tokenHandshakeInterceptor);
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
