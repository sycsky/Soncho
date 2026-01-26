package com.example.aikef.service;

import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.dto.MessageDto;
import com.example.aikef.dto.AgentDto;
import com.example.aikef.dto.request.SendMessageRequest;
import com.example.aikef.dto.request.UpdateSessionStatusRequest;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.AgentStatus;
import com.example.aikef.model.enums.SessionAction;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CustomerPrincipal;
import com.example.aikef.websocket.WebSocketSessionManager;
import com.example.aikef.workflow.service.AiWorkflowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.aikef.saas.context.TenantContext;

@Service
public class WebSocketEventService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventService.class);

    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final AgentService agentService;
    private final WebSocketSessionManager sessionManager;
    private final AgentMentionService agentMentionService;
    private final MessageRepository messageRepository;
    private final AiWorkflowService aiWorkflowService;
    private final SessionMessageGateway messageGateway;

    @Autowired
    private  CustomerService customerService;

    @Autowired
    private ReadRecordService readRecordService;
    
    @Autowired
    @Lazy
    private ExternalPlatformService externalPlatformService;
    
    @Autowired
    @Lazy
    private com.example.aikef.workflow.service.WorkflowExecutionScheduler workflowScheduler;

    @Autowired
    @Lazy
    private OfficialChannelMessageService officialChannelMessageService;

    @Autowired
    @Lazy
    private ChatSessionService chatSessionService;

    @Autowired
    public WebSocketEventService(ObjectMapper objectMapper,
                                 @Lazy ConversationService conversationService,
                                 @Lazy WebSocketSessionManager sessionManager,
                                 @Lazy AgentService agentService,
                                 @Lazy AgentMentionService agentMentionService,
                                 MessageRepository messageRepository,
                                 @Lazy AiWorkflowService aiWorkflowService,
                                 @Lazy SessionMessageGateway messageGateway) {

        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
        this.sessionManager = sessionManager;
        this.agentService = agentService;
        this.agentMentionService = agentMentionService;
        this.messageRepository = messageRepository;
        this.aiWorkflowService = aiWorkflowService;
        this.messageGateway = messageGateway;
    }

    public ServerEvent handle(String event, JsonNode payload, AgentPrincipal agentPrincipal, CustomerPrincipal customerPrincipal) throws JsonProcessingException {
        return switch (event) {
            case "sendMessage" -> handleSendMessage(payload, agentPrincipal, customerPrincipal);
            case "updateSessionStatus" -> handleSessionStatus(payload);
            case "agentTyping" -> handleAgentTyping(payload, agentPrincipal);
            case "changeAgentStatus" -> handleChangeAgentStatus(payload, agentPrincipal);
            case "ping"-> new ServerEvent("ping", Map.of(
                    "message", "ping"));
            default -> new ServerEvent("notification", Map.of(
                    "type", "WARN",
                    "message", "未知事件: " + event));
        };
    }

    private ServerEvent handleSendMessage(JsonNode payload, AgentPrincipal agentPrincipal, CustomerPrincipal customerPrincipal) throws JsonProcessingException {
        SendMessageRequest request = objectMapper.treeToValue(payload, SendMessageRequest.class);
        UUID sessionId = UUID.fromString(request.sessionId());

        // 根据 Session ID 设置租户上下文
        setTenantContextFromSession(sessionId);

        try {
            UUID agentId = agentPrincipal != null ? agentPrincipal.getId() : null;
            UUID customerId = customerPrincipal != null ? customerPrincipal.getId() : null;
            UUID senderId = agentId != null ? agentId : customerId;

            if (agentId != null) {
                readRecordService.updateReadTime(sessionId, agentId);
            }
            
            // 如果是客户发送的消息，检查并重新打开会话
            if (customerId != null && agentId == null) {
                chatSessionService.checkAndReopenResolvedSession(sessionId);
            }
            
            // 保存消息并获取完整的消息数据
            MessageDto messageDto = conversationService.sendMessage(request, agentId);

            // 如果有被@的客服，创建@记录
            if (request.mentions() != null && !request.mentions().isEmpty()) {
                // 获取消息实体
                Message message = messageRepository.findById(messageDto.id()).orElse(null);
                
                // 转换 mention 字符串为 UUID 列表
                List<UUID> mentionAgentIds = request.mentions().stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
                
                // 创建@记录
                agentMentionService.createMentions(mentionAgentIds, sessionId, message);
            }
            
            // 广播消息给会话的所有参与者（除了发送者）
            broadcastMessageToSession(sessionId, messageDto, senderId);
            
            // 如果是客户发送的消息，检查是否需要触发 AI 工作流
            if (customerId != null && agentId == null) {
                // 检查是否为纯图片消息 (文本为空 且 包含图片附件)
                boolean isImageOnly = (request.text() == null || request.text().isBlank()) &&
                                      request.attachments() != null && 
                                      !request.attachments().isEmpty() &&
                                      request.attachments().stream().allMatch(att -> att.type() == com.example.aikef.model.enums.AttachmentType.IMAGE);
                
                if (isImageOnly) {
                    log.info("消息仅包含图片，跳过 AI 工作流: sessionId={}, messageId={}", sessionId, messageDto.id());
                } else {
                    triggerAiWorkflowIfNeeded(sessionId, request.text(), messageDto.id());
                }
            }
            
            // 如果是客服发送的消息，检查是否需要转发到第三方平台
            if (agentId != null) {
                // 构建附件列表
                List<Attachment> attachments = null;
                if (request.attachments() != null && !request.attachments().isEmpty()) {
                    attachments = request.attachments().stream()
                            .map(attPayload -> {
                                Attachment att = new Attachment();
                                att.setType(attPayload.type());
                                att.setUrl(attPayload.url());
                                att.setName(attPayload.name());
                                att.setSizeKb(attPayload.sizeKb());
                                return att;
                            })
                            .collect(Collectors.toList());
                }

                // 先尝试官方渠道（通过SDK，支持附件）
                boolean sentToOfficial = officialChannelMessageService.sendMessageToOfficialChannel(
                        sessionId, request.text(), SenderType.AGENT,messageDto.messageType(), attachments);
                
                if (!sentToOfficial) {
                    // 如果不是官方渠道，使用原有的外部平台方式（支持附件）
                    externalPlatformService.forwardMessageToExternalPlatform(
                            sessionId, request.text(), SenderType.AGENT, attachments);
                }
            }
            
            return new ServerEvent("newMessage", Map.of(
                    "sessionId", messageDto.sessionId(),
                    "message", messageDto));
        } finally {
            // 清理租户上下文
            TenantContext.clear();
        }
    }

    @Value("${app.saas.enabled:false}")
    private boolean saasEnabled;

    private void setTenantContextFromSession(UUID sessionId) {

        if(!saasEnabled) {return;}
        try {
            ChatSession session = conversationService.getChatSession(sessionId);
            if (session != null && session.getTenantId() != null) {
                TenantContext.setTenantId(session.getTenantId());
                log.debug("WebSocket: 已为会话 {} 设置租户上下文: {}", sessionId, session.getTenantId());
            } else {
                log.warn("WebSocket: 无法获取会话 {} 的租户信息", sessionId);
            }
        } catch (Exception e) {
            log.error("WebSocket: 设置租户上下文失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }



    /**
     * 触发 AI 工作流处理客户消息
     * 仅在会话状态为 AI_HANDLING 时执行
     * 使用调度服务实现防抖和队列功能
     */
    public void triggerAiWorkflowIfNeeded(UUID sessionId, String userMessage, UUID messageId) {
        try {
            // 获取会话状态
            ChatSession session = conversationService.getChatSession(sessionId);
            if (session == null) {
                log.warn("会话不存在，跳过工作流: sessionId={}", sessionId);
                return;
            }

            // 只有 AI_HANDLING 状态才执行工作流
            if (session.getStatus() == SessionStatus.HUMAN_HANDLING) {
                log.debug("会话状态不是 AI_HANDLING，跳过工作流: sessionId={}, status={}", 
                        sessionId, session.getStatus());
                return;
            }

            log.debug("提交消息到工作流调度器: sessionId={}, messageId={}, message={}", sessionId, messageId,
                    userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);

            // 使用调度服务提交消息（带防抖和队列）
            workflowScheduler.submitMessage(sessionId, userMessage, messageId);

        } catch (Exception e) {
            log.error("触发 AI 工作流失败: sessionId={}", sessionId, e);
        }
    }



    public ServerEvent handleSessionStatus(JsonNode payload) throws JsonProcessingException {
        UpdateSessionStatusRequest request = objectMapper.treeToValue(payload, UpdateSessionStatusRequest.class);
        ChatSession session = conversationService.updateSessionStatus(request);
        ChatSessionDto sessionDto = conversationService.getChatSessionDto(session.getId());
        return new ServerEvent("sessionUpdated", Map.of(
                "session", Map.of(
                        "customer",sessionDto.user(),
                        "id", session.getId(),
                        "status", session.getStatus(),
                        "primaryAgentId", sessionDto.primaryAgentId() != null ? sessionDto.primaryAgentId() : null)));
    }

    

    private ServerEvent handleAgentTyping(JsonNode payload, AgentPrincipal principal) throws JsonProcessingException {
        String sessionId = payload.hasNonNull("sessionId") ? payload.get("sessionId").asText() : null;
        return new ServerEvent("agentTyping", Map.of(
                "sessionId", sessionId,
                "agentId", principal != null ? principal.getId() : null,
                "isTyping", true));
    }
    
    private ServerEvent handleChangeAgentStatus(JsonNode payload, AgentPrincipal principal) throws JsonProcessingException {
        if (principal == null) {
            return new ServerEvent("error", Map.of(
                    "type", "UNAUTHORIZED",
                    "message", "需要坐席认证"));
        }
        if (payload == null || !payload.hasNonNull("status")) {
            return new ServerEvent("error", Map.of(
                    "type", "INVALID_ARGUMENT",
                    "message", "缺少 status 字段"));
        }
        String statusText = payload.get("status").asText();
        AgentStatus status;
        try {
            status = AgentStatus.valueOf(statusText);
        } catch (IllegalArgumentException ex) {
            return new ServerEvent("error", Map.of(
                    "type", "INVALID_STATUS",
                    "message", "不合法的坐席状态: " + statusText));
        }
        AgentDto updated = agentService.updateAgent(principal.getId(), new com.example.aikef.dto.request.UpdateAgentRequest(null, null, status, null, null));
        return new ServerEvent("agentStatusChanged", Map.of(
                "agentId", updated.id(),
                "status", updated.status()));
    }
    
    /**
     * 广播消息给会话的所有参与者（除了发送者）
     */
    private void broadcastMessageToSession(UUID chatSessionId, MessageDto messageDto, UUID senderId) throws JsonProcessingException {
        // 从 ConversationService 获取会话信息
        ChatSession session = conversationService.getChatSession(chatSessionId);

        
        if (session == null) {
            return;
        }
        
        // 构建广播事件
        ServerEvent broadcastEvent = new ServerEvent("newMessage", Map.of(
                "sessionId", messageDto.sessionId(),
                "message", messageDto));
        
        String broadcastMessage = objectMapper.writeValueAsString(broadcastEvent);
        
        // 广播给会话的所有参与者（除了发送者）
        sessionManager.broadcastToSession(
                chatSessionId,
                session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null,
                session.getSupportAgentIds() != null ? session.getSupportAgentIds().stream().toList() : null,
                session.getCustomer() != null ? session.getCustomer().getId() : null,
                senderId,  // 发送者ID（可能是客服或客户）
                broadcastMessage
        );
    }
}
