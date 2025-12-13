package com.example.aikef.websocket;

import com.example.aikef.dto.ChatMessageDto;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.dto.websocket.WebSocketEnvelope;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import com.example.aikef.service.OfflineMessageService;
import com.example.aikef.service.WebSocketEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final WebSocketEventService eventService;
    private final OfflineMessageService offlineMessageService;
    private final WebSocketSessionManager sessionManager;

    @Autowired
    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                @Lazy WebSocketEventService eventService,
                                OfflineMessageService offlineMessageService,
                                @Lazy WebSocketSessionManager sessionManager) {
        this.objectMapper = objectMapper;
        this.eventService = eventService;
        this.offlineMessageService = offlineMessageService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AgentPrincipal agentPrincipal = (AgentPrincipal) session.getAttributes().get("AGENT_PRINCIPAL");
        CustomerPrincipal customerPrincipal = (CustomerPrincipal) session.getAttributes().get("CUSTOMER_PRINCIPAL");
        
        if (agentPrincipal != null) {
            log.info("✅ 坐席 WebSocket 连接建立成功: sessionId={}, 用户={}, agentId={}", 
                    session.getId(), agentPrincipal.getUsername(), agentPrincipal.getId());
            // 注册客服连接
            sessionManager.registerAgent(agentPrincipal.getId(), session);
            // 推送离线消息给客服
            pushOfflineMessagesToAgent(session, agentPrincipal.getId());
        } else if (customerPrincipal != null) {
            log.info("✅ 客户 WebSocket 连接建立成功: sessionId={}, 客户={}, customerId={}, 渠道={}", 
                    session.getId(), customerPrincipal.getName(), 
                    customerPrincipal.getId(), customerPrincipal.getChannel());
            // 注册客户连接（不推送离线消息，客户通过历史消息接口获取）
            sessionManager.registerCustomer(customerPrincipal.getId(), session);
        } else {
            log.warn("⚠️ WebSocket 连接建立但未找到认证信息: sessionId={}, URI={}", 
                    session.getId(), session.getUri());
        }
    }

    /**
     * 推送离线消息给客服
     */
    private void pushOfflineMessagesToAgent(WebSocketSession session, UUID agentId) {
        try {
            List<ChatMessageDto> unsentMessages = offlineMessageService.getUnsentMessagesForAgent(agentId);
            
            if (!unsentMessages.isEmpty()) {
                log.info("📬 推送 {} 条离线消息给客服: agentId={}", unsentMessages.size(), agentId);
                
                for (ChatMessageDto message : unsentMessages) {
                    // 发送离线消息
                    Map<String, Object> offlineMsg = new HashMap<>();
                    offlineMsg.put("type", "offline_message");
                    offlineMsg.put("message", message);
                    
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(offlineMsg)));
                }
                
                // 推送完成通知
                Map<String, Object> completeMsg = new HashMap<>();
                completeMsg.put("type", "offline_messages_complete");
                completeMsg.put("count", unsentMessages.size());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(completeMsg)));
                
                // 标记为已发送
                offlineMessageService.markAsSentForAgent(agentId);
                log.info("✅ 已标记离线消息为已发送 (客服): agentId={}", agentId);
            }
        } catch (Exception e) {
            log.error("❌ 推送离线消息失败 (客服): agentId={}", agentId, e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("📨 收到 WebSocket 消息: sessionId={}, 消息长度={}", 
                session.getId(), message.getPayload().length());
        
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            
            // 所有消息必须是事件格式，必须包含 event 字段
            if (!jsonNode.hasNonNull("event")) {
                log.warn("❌ 消息格式错误: sessionId={}, 缺少 event 字段", session.getId());
                ServerEvent errorEvent = new ServerEvent("error", Map.of(
                        "type", "INVALID_FORMAT",
                        "message", "消息格式错误：必须包含 event 字段"));
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorEvent)));
                return;
            }
            
            WebSocketEnvelope envelope = objectMapper.treeToValue(jsonNode, WebSocketEnvelope.class);
            AgentPrincipal agentPrincipal = resolveAgentPrincipalFromSession(session);
            CustomerPrincipal customerPrincipal = resolveCustomerPrincipalFromSession(session);
            log.debug("处理事件消息: event={}", envelope.event());
            ServerEvent serverEvent = eventService.handle(envelope.event(), envelope.payload(), agentPrincipal, customerPrincipal);
            
            // sendMessage 事件已在 handleSendMessage 中广播，不需要再返回给发送者
            // 其他事件需要返回响应
            if (!"sendMessage".equals(envelope.event())) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(serverEvent)));
            }
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            log.warn("❌ 消息格式异常: sessionId={}, 错误={}", session.getId(), ex.getMessage());
            ServerEvent errorEvent = new ServerEvent("error", Map.of(
                    "type", "PARSE_ERROR",
                    "message", "消息解析失败: " + ex.getMessage()));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorEvent)));
        } catch (Exception ex) {
            log.error("❌ 处理消息时发生异常: sessionId={}", session.getId(), ex);
            ServerEvent errorEvent = new ServerEvent("error", Map.of(
                    "type", "SERVER_ERROR",
                    "message", "服务器内部错误"));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorEvent)));
        }
    }

    private AgentPrincipal resolvePrincipal(Principal principal) {
        // 首先尝试从 Principal 获取
        if (principal instanceof AgentPrincipal agentPrincipal) {
            return agentPrincipal;
        }
        return null;
    }

    private AgentPrincipal resolveAgentPrincipalFromSession(WebSocketSession session) {
        // 从握手属性中获取
        Object attr = session.getAttributes().get("AGENT_PRINCIPAL");
        if (attr instanceof AgentPrincipal agentPrincipal) {
            return agentPrincipal;
        }
        // 如果属性中没有，尝试从 Principal 获取
        return resolvePrincipal(session.getPrincipal());
    }
    
    private CustomerPrincipal resolveCustomerPrincipalFromSession(WebSocketSession session) {
        // 从握手属性中获取
        Object attr = session.getAttributes().get("CUSTOMER_PRINCIPAL");
        if (attr instanceof CustomerPrincipal customerPrincipal) {
            return customerPrincipal;
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AgentPrincipal agentPrincipal = (AgentPrincipal) session.getAttributes().get("AGENT_PRINCIPAL");
        CustomerPrincipal customerPrincipal = (CustomerPrincipal) session.getAttributes().get("CUSTOMER_PRINCIPAL");
        
        // 移除连接
        sessionManager.removeSession(session);
        
        String userInfo = "未知用户";
        if (agentPrincipal != null) {
            userInfo = "坐席: " + agentPrincipal.getUsername();
        } else if (customerPrincipal != null) {
            userInfo = "客户: " + customerPrincipal.getName();
        }
        
        if (status.getCode() == 1000) {
            log.info("🔌 WebSocket 正常关闭: sessionId={}, 用户={}, 状态码={}, 原因={}", 
                    session.getId(), userInfo, status.getCode(), 
                    status.getReason() != null ? status.getReason() : "客户端主动关闭");
        } else {
            log.warn("❌ WebSocket 异常关闭: sessionId={}, 用户={}, 状态码={}, 原因={}", 
                    session.getId(), userInfo, status.getCode(), status.getReason());
        }
    }
}