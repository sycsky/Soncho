package com.example.aikef.websocket;

import com.example.aikef.model.ChatSession;
import com.example.aikef.service.AiKnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket 会话管理器
 * 管理所有在线用户的 WebSocket 连接
 */
@Component
@Lazy
public class WebSocketSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    // 客服ID -> WebSocket会话列表（一个客服可能有多个设备连接）
    private final Map<UUID, Set<WebSocketSession>> agentSessions = new ConcurrentHashMap<>();
    
    // 客户ID -> WebSocket会话列表
    private final Map<UUID, Set<WebSocketSession>> customerSessions = new ConcurrentHashMap<>();

    @Autowired
    private  AiKnowledgeService aiKnowledgeService;
    /**
     * 注册客服连接
     */
    public void registerAgent(UUID agentId, WebSocketSession session) {
        agentSessions.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("📝 注册客服连接: agentId={}, sessionId={}, 总连接数={}", 
                agentId, session.getId(), agentSessions.get(agentId).size());
    }

    /**
     * 注册客户连接
     */
    public void registerCustomer(UUID customerId, WebSocketSession session) {
        customerSessions.computeIfAbsent(customerId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("📝 注册客户连接: customerId={}, sessionId={}, 总连接数={}", 
                customerId, session.getId(), customerSessions.get(customerId).size());
    }

    /**
     * 移除连接
     */
    public void removeSession(WebSocketSession session) {
        // 从客服连接中移除
        agentSessions.values().forEach(sessions -> sessions.remove(session));
        agentSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // 从客户连接中移除
        customerSessions.values().forEach(sessions -> sessions.remove(session));
        customerSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        log.debug("🗑️ 移除连接: sessionId={}", session.getId());
    }

    /**
     * 发送消息给指定客服（所有设备）
     */
    public void sendToAgent(UUID agentId, String message) {
        Set<WebSocketSession> sessions = agentSessions.get(agentId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("客服不在线: agentId={}", agentId);
            return;
        }

        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.debug("✉️ 发送消息给客服: agentId={}, sessionId={}", agentId, session.getId());
                } catch (SockJsTransportFailureException e) {
                    log.debug("SockJS 发送失败，移除异常连接: agentId={}, sessionId={}", agentId, session.getId());
                    removeSession(session);
                } catch (Exception e) {
                    log.warn("⚠️ 发送消息给客服失败，移除异常连接: agentId={}, sessionId={}", agentId, session.getId(), e);
                    removeSession(session);
                }
            }
        });
    }

    /**
     * 发送消息给指定客户（所有设备）
     */
    public void sendToCustomer(UUID customerId, String message) {
        Set<WebSocketSession> sessions = customerSessions.get(customerId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("客户不在线: customerId={}", customerId);
            return;
        }

        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.debug("✉️ 发送消息给客户: customerId={}, sessionId={}", customerId, session.getId());
                } catch (SockJsTransportFailureException e) {
                    log.debug("SockJS 发送失败，移除异常连接: customerId={}, sessionId={}", customerId, session.getId());
                    removeSession(session);
                } catch (Exception e) {
                    log.warn("⚠️ 发送消息给客户失败，移除异常连接: customerId={}, sessionId={}", customerId, session.getId(), e);
                    removeSession(session);
                }
            }
        });
    }

    /**
     * 广播消息给会话的所有参与者（除了发送者）
     * 
     * @param chatSessionId 聊天会话ID
     * @param primaryAgentId 主责客服ID
     * @param supportAgentIds 支持客服ID列表
     * @param customerId 客户ID
     * @param senderId 发送者ID（客服或客户）
     * @param message 消息内容（JSON字符串）
     */
    public void broadcastToSession(UUID chatSessionId, 
                                   UUID primaryAgentId, 
                                   List<UUID> supportAgentIds,
                                   UUID customerId,
                                   UUID senderId,
                                   String message) {
        
        log.debug("📢 广播消息到会话: chatSessionId={}, senderId={}", chatSessionId, senderId);
        
        // 发送给主责客服（如果不是发送者）
        if (primaryAgentId != null && !primaryAgentId.equals(senderId)) {
            sendToAgent(primaryAgentId, message);
        }
        
        // 发送给所有支持客服（如果不是发送者）
        if (supportAgentIds != null) {
            supportAgentIds.stream()
                    .filter(agentId -> !agentId.equals(senderId))
                    .forEach(agentId -> sendToAgent(agentId, message));
        }
        
        // 发送给客户（如果不是发送者）
        if (customerId != null && !customerId.equals(senderId)) {
            sendToCustomer(customerId, message);
        }

        if(customerId.equals(senderId)) {
            aiKnowledgeService.suggestTags(chatSessionId.toString());
        }
    }

    /**
     * 检查客服是否在线
     */
    public boolean isAgentOnline(UUID agentId) {
        Set<WebSocketSession> sessions = agentSessions.get(agentId);
        return sessions != null && !sessions.isEmpty() && 
               sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * 检查客户是否在线
     */
    public boolean isCustomerOnline(UUID customerId) {
        Set<WebSocketSession> sessions = customerSessions.get(customerId);
        return sessions != null && !sessions.isEmpty() && 
               sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * 获取在线客服数量
     */
    public int getOnlineAgentCount() {
        return (int) agentSessions.values().stream()
                .filter(sessions -> sessions.stream().anyMatch(WebSocketSession::isOpen))
                .count();
    }

    /**
     * 获取在线客户数量
     */
    public int getOnlineCustomerCount() {
        return (int) customerSessions.values().stream()
                .filter(sessions -> sessions.stream().anyMatch(WebSocketSession::isOpen))
                .count();
    }
    
    /**
     * 获取会话中在线的客服ID集合
     * 
     * @param session 聊天会话
     * @return 在线客服ID集合
     */
    public Set<UUID> getOnlineAgentsInSession(ChatSession session) {
        Set<UUID> onlineAgents = new HashSet<>();
        
        // 检查主责客服是否在线
        if (session.getPrimaryAgent() != null && isAgentOnline(session.getPrimaryAgent().getId())) {
            onlineAgents.add(session.getPrimaryAgent().getId());
        }
        
        // 检查支持客服是否在线
        if (session.getSupportAgentIds() != null) {
            onlineAgents.addAll(
                session.getSupportAgentIds().stream()
                    .filter(this::isAgentOnline)
                    .collect(Collectors.toSet())
            );
        }
        
        return onlineAgents;
    }
}