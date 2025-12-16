package com.example.aikef.service.channel.line;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.service.OfficialChannelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Line官方账号适配器
 * 处理Line官方账号的消息接收和发送（通过Line官方SDK）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LineOfficialAdapter {

    private final OfficialChannelService channelService;
    private final ObjectMapper objectMapper;

    /**
     * 验证Line Webhook签名
     */
    public boolean verifySignature(OfficialChannelConfig config, String body, String signature) {
        try {
            Map<String, Object> configData = channelService.parseConfigJson(config);
            String channelSecret = (String) configData.get("channelSecret");
            
            if (channelSecret == null) {
                log.warn("Line配置中缺少channelSecret");
                return false;
            }
            
            // Line签名验证：使用HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    channelSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            
            return calculatedSignature.equals(signature);
            
        } catch (Exception e) {
            log.error("验证Line签名失败", e);
            return false;
        }
    }

    /**
     * 解析Line消息
     */
    public Map<String, Object> parseMessage(String body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> lineMessage = objectMapper.readValue(body, Map.class);
            log.info("解析Line消息: events数量={}", 
                    lineMessage.containsKey("events") ? 
                    ((java.util.List<?>) lineMessage.get("events")).size() : 0);
            return lineMessage;
        } catch (Exception e) {
            log.error("解析Line消息失败", e);
            throw new RuntimeException("解析Line消息失败", e);
        }
    }

    /**
     * 将Line消息转换为统一的WebhookMessageRequest格式
     */
    public WebhookMessageRequest toWebhookRequest(Map<String, Object> lineMessage) {
        // Line消息格式：{"events": [{"type": "message", "source": {"userId": "xxx"}, "message": {"text": "xxx"}}]}
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> events = 
                (java.util.List<Map<String, Object>>) lineMessage.get("events");
        
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Line消息中没有events");
        }
        
        Map<String, Object> event = events.get(0);
        String eventType = (String) event.get("type");
        
        if (!"message".equals(eventType)) {
            log.warn("忽略非消息类型事件: type={}", eventType);
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) event.get("source");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) event.get("message");
        
        String userId = (String) source.get("userId");
        String text = (String) message.get("text");
        String replyToken = (String) event.get("replyToken");
        
        return new WebhookMessageRequest(
                userId,  // threadId（Line使用userId作为threadId）
                text,    // content
                "text",  // messageType
                userId,  // externalUserId
                userId,  // userName（需要从用户信息获取）
                null,    // email
                null,    // phone
                null,    // categoryId
                null,    // attachmentUrl
                null,    // attachmentName
                System.currentTimeMillis(), // timestamp
                null,    // language
                Map.of("replyToken", replyToken != null ? replyToken : "") // metadata
        );
    }

    /**
     * 发送消息到Line官方账号（通过Line官方SDK）
     */
    public void sendMessage(OfficialChannelConfig config, String userId, String content) {
        // TODO: 使用Line SDK发送消息
        // 1. 从configJson中获取channelAccessToken
        // 2. 调用Line API发送消息
        // POST https://api.line.me/v2/bot/message/push
        
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String channelAccessToken = (String) configData.get("channelAccessToken");
        
        log.info("发送消息到Line官方账号: userId={}, content={}", 
                userId, content.length() > 50 ? content.substring(0, 50) + "..." : content);
        
        // TODO: 实现Line消息发送逻辑
        // 需要使用Line SDK或直接调用Line API
    }
}

