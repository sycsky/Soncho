package com.example.aikef.service;

import com.example.aikef.model.Agent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnrepliedMessageReminderService {

    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final ResendEmailService resendEmailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.unreplied-reminder.enabled:true}")
    private boolean enabled;

    @Value("${app.unreplied-reminder.threshold-minutes:5}")
    private long thresholdMinutes;

    @Value("${app.unreplied-reminder.max-age-minutes:60}")
    private long maxAgeMinutes;

    private static final String REDIS_KEY_PREFIX = "unreplied_reminder:last_message:";
    private static final Duration REDIS_TTL = Duration.ofDays(7);

    @Scheduled(fixedDelayString = "${app.unreplied-reminder.interval-ms:60000}")
    @Transactional(readOnly = true)
    public void scanUnrepliedSessions() {
        if (!enabled) {
            return;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(1));
        Instant maxAgeCutoff = now.minus(Duration.ofMinutes(maxAgeMinutes));
        List<ChatSession> sessions = chatSessionRepository.findByLastActiveAtBefore(cutoff);
        if (sessions.isEmpty()) {
            return;
        }

        for (ChatSession session : sessions) {
            UUID sessionId = session.getId();
            Message lastMessage = messageRepository.findFirstBySession_IdOrderByCreatedAtDesc(sessionId);
            if (lastMessage == null || lastMessage.getSenderType() != SenderType.USER) {
                deleteNotifiedMessage(session);
                continue;
            }

            Instant lastMessageTime = lastMessage.getCreatedAt();
            if (lastMessageTime == null || lastMessageTime.isAfter(cutoff)) {
                continue;
            }
            if (lastMessageTime.isBefore(maxAgeCutoff)) {
                continue;
            }

            UUID lastNotifiedMessageId = getNotifiedMessageId(session);
            if (lastMessage.getId() != null && lastMessage.getId().equals(lastNotifiedMessageId)) {
                continue;
            }

            Agent agent = session.getPrimaryAgent();
            if (agent == null || agent.getEmail() == null || agent.getEmail().isBlank()) {
                log.warn("Primary agent or email missing, skip reminder: sessionId={}", sessionId);
                continue;
            }

            String subject = "Unreplied customer message";
            String htmlContent = buildHtmlContent(session, lastMessage, cutoff);
            String textContent = buildTextContent(session, lastMessage, cutoff);
            log.info("Sending reminder for sessionId={}", sessionId);
            boolean sent = resendEmailService.sendEmail(agent.getEmail(), subject, htmlContent, textContent);
            if (sent) {
                if (lastMessage.getId() != null) {
                    saveNotifiedMessage(session, lastMessage.getId());
                }
                log.info("Unreplied message reminder sent: sessionId={}, agentId={}", sessionId, agent.getId());
            }
        }
    }

    private String buildTextContent(ChatSession session, Message message, Instant cutoff) {
        String customerName = getCustomerName(session.getCustomer());
        String preview = buildMessagePreview(message);
        return """
                You have an unreplied customer message.

                Session ID: %s
                Customer: %s
                Message time: %s
                Message preview: %s

                Please reply as soon as possible.
                """.formatted(
                session.getId(),
                customerName,
                message.getCreatedAt(),
                preview
        );
    }

    private String buildHtmlContent(ChatSession session, Message message, Instant cutoff) {
        String customerName = getCustomerName(session.getCustomer());
        String preview = buildMessagePreview(message);
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #1a73e8; color: white; padding: 16px; border-radius: 6px 6px 0 0; }
                        .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 6px 6px; }
                        .label { font-weight: bold; }
                        .preview { background-color: #ffffff; padding: 12px; border-radius: 6px; border: 1px solid #e0e0e0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>Unreplied customer message</h2>
                        </div>
                        <div class="content">
                            <p><span class="label">Session ID:</span> %s</p>
                            <p><span class="label">Customer:</span> %s</p>
                            <p><span class="label">Message time:</span> %s</p>
                            <p><span class="label">Message preview:</span></p>
                            <div class="preview">%s</div>
                            <p>Please reply as soon as possible.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                session.getId(),
                customerName,
                message.getCreatedAt(),
                escapeHtml(preview)
        );
    }

    private String getCustomerName(Customer customer) {
        if (customer == null) {
            return "Unknown";
        }
        if (customer.getName() != null && !customer.getName().isBlank()) {
            return customer.getName();
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return customer.getEmail();
        }
        return "Unknown";
    }

    private String buildMessagePreview(Message message) {
        String text = message.getText();
        if (text != null && !text.isBlank()) {
            String trimmed = text.trim();
            return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
        }
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            return "Attachment only message";
        }
        return "Empty message";
    }

    private String escapeHtml(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private UUID getNotifiedMessageId(ChatSession session) {
        String key = buildRedisKey(session);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void saveNotifiedMessage(ChatSession session, UUID messageId) {
        String key = buildRedisKey(session);
        redisTemplate.opsForValue().set(key, messageId.toString(), REDIS_TTL);
    }

    private void deleteNotifiedMessage(ChatSession session) {
        String key = buildRedisKey(session);
        redisTemplate.delete(key);
    }

    private String buildRedisKey(ChatSession session) {
        String tenantId = session.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        return REDIS_KEY_PREFIX + tenantId + ":" + session.getId();
    }
}

