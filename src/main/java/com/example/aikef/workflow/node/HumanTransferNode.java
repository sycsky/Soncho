package com.example.aikef.workflow.node;

import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.service.ChatSessionService;
import com.example.aikef.service.ConversationService;
import com.example.aikef.service.CustomerService;
import com.example.aikef.service.SessionMessageGateway;
import com.example.aikef.websocket.WebSocketSessionManager;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 转人工节点
 * 直接切换当前会话为人工处理状态
 */
@LiteflowComponent("human_transfer")
public class HumanTransferNode extends BaseWorkflowNode {

    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    @Autowired
    private SessionMessageGateway messageGateway;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationService conversationService;

    @Override
    @Transactional
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 获取转人工原因和消息
            String reason = getConfigString("reason", "用户请求转人工");
            String message = getConfigString("message", "已经为您转接到人工");
            
            // 设置转人工标记（兼容旧逻辑）
            ctx.setNeedHumanTransfer(true);
            ctx.setHumanTransferReason(reason);
            
            // 直接更新会话状态为人工处理
            if (ctx.getSessionId() != null) {
                ChatSession session = chatSessionRepository.findById(ctx.getSessionId()).orElse(null);
                if (session != null) {
                    session.setStatus(SessionStatus.HUMAN_HANDLING);
                    chatSessionRepository.save(session);
                    log.info("会话状态已切换为人工处理: sessionId={}, reason={}", ctx.getSessionId(), reason);
                    ChatSessionDto sessionDto = conversationService.getChatSessionDto(ctx.getSessionId());
                    // 广播状态更新事件到 WebSocket
                    try {
                        ServerEvent updateEvent = new ServerEvent("sessionUpdated", Map.of(
                                "session", Map.of(
                                        "id", session.getId(),
                                        "status", session.getStatus(),
                                         "customer", sessionDto.user(),
                                        "primaryAgentId", sessionDto.primaryAgentId() != null ? sessionDto.primaryAgentId() : null,
                                        "reason", reason
                                )));
                        
                        String jsonMessage = objectMapper.writeValueAsString(updateEvent);
                        
                        // 广播给会话的所有参与者
                        sessionManager.broadcastToSession(
                                session.getId(),
                                session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null,
                                session.getSupportAgentIds() != null ? session.getSupportAgentIds().stream().toList() : null,
                                session.getCustomer() != null ? session.getCustomer().getId() : null,
                                null, // senderId 为 null，表示系统发送，广播给所有人
                                jsonMessage
                        );
                    } catch (Exception e) {
                        log.warn("广播会话状态更新失败: sessionId={}", ctx.getSessionId(), e);
                    }
                } else {
                    log.warn("会话不存在，无法切换状态: sessionId={}", ctx.getSessionId());
                }
            } else {
                log.warn("会话ID为空，无法切换状态");
            }
            
            // 设置转人工提示消息
            ctx.setFinalReply(message);
            
            setOutput(message);
            recordExecution(reason, message, startTime, true, null);
            
        } catch (Exception e) {
            log.error("转人工节点执行失败", e);
            String errorMessage = "转接人工客服时出现问题，请稍后重试。";
            ctx.setFinalReply(errorMessage);
            setOutput(errorMessage);
            recordExecution(null, errorMessage, startTime, false, e.getMessage());
        }
    }
}

