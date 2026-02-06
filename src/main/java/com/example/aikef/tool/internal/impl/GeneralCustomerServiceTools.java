package com.example.aikef.tool.internal.impl;

import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.service.ConversationService;
import com.example.aikef.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class GeneralCustomerServiceTools {

    private final ChatSessionRepository chatSessionRepository;
    private final WebSocketSessionManager sessionManager;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @Tool("This tool can be used if a user needs to be transferred to human assistance, but the user must be asked for confirmation before use.")
    @Transactional
    public String transferToCustomerService(
            @P(value = "The reason for transferring to human agent", required = true) String reason,
            @P(value = "Current Session ID", required = true) String sessionId
    ) {
        log.info("Executing transferToCustomerService tool. sessionId={}, reason={}", sessionId, reason);
        
        try {
            UUID uuid = UUID.fromString(sessionId);
            ChatSession session = chatSessionRepository.findById(uuid).orElse(null);
            
            if (session == null) {
                return "Error: Session not found: " + sessionId;
            }

            // Update session status
            session.setStatus(SessionStatus.HUMAN_HANDLING);
            chatSessionRepository.save(session);
            log.info("Session status updated to HUMAN_HANDLING: sessionId={}, reason={}", sessionId, reason);

            // Broadcast update
            try {
                ChatSessionDto sessionDto = conversationService.getChatSessionDto(uuid);
                ServerEvent updateEvent = new ServerEvent("sessionUpdated", Map.of(
                        "session", Map.of(
                                "id", session.getId(),
                                "status", session.getStatus(),
                                "customer", sessionDto.user(),
                                "primaryAgentId", sessionDto.primaryAgentId() != null ? sessionDto.primaryAgentId() : null,
                                "reason", reason
                        )));

                String jsonMessage = objectMapper.writeValueAsString(updateEvent);

                sessionManager.broadcastToSession(
                        session.getId(),
                        session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null,
                        session.getSupportAgentIds() != null ? session.getSupportAgentIds().stream().toList() : null,
                        session.getCustomer() != null ? session.getCustomer().getId() : null,
                        null, // senderId is null (system message)
                        jsonMessage
                );
                
                return "Successfully transferred to human agent. Reason: " + reason;
                
            } catch (Exception e) {
                log.warn("Failed to broadcast session update: sessionId={}", sessionId, e);
                return "Transferred to human agent, but failed to notify clients: " + e.getMessage();
            }

        } catch (IllegalArgumentException e) {
            return "Error: Invalid Session ID format: " + sessionId;
        } catch (Exception e) {
            log.error("Failed to transfer to customer service", e);
            return "Error: " + e.getMessage();
        }
    }
}
