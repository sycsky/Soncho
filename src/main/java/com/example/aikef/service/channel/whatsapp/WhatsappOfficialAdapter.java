package com.example.aikef.service.channel.whatsapp;

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
import com.example.aikef.model.Attachment;
import java.util.List;
import java.util.Map;

/**
 * WhatsApp Business适配器
 * 处理WhatsApp Business的消息接收和发送（通过WhatsApp官方SDK）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsappOfficialAdapter {

    private final OfficialChannelService channelService;
    private final ObjectMapper objectMapper;

    /**
     * 验证WhatsApp Webhook签名
     */
    public boolean verifySignature(OfficialChannelConfig config, String body, String signature) {
        try {
            Map<String, Object> configData = channelService.parseConfigJson(config);
            String appSecret = (String) configData.get("appSecret");
            
            if (appSecret == null) {
                log.warn("WhatsApp配置中缺少appSecret");
                return false;
            }
            
            // WhatsApp签名验证：使用HMAC-SHA256
            // 格式：sha256=xxx
            if (signature != null && signature.startsWith("sha256=")) {
                String expectedSignature = signature.substring(7);
                
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(
                        appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(secretKeySpec);
                
                byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
                String calculatedSignature = Base64.getEncoder().encodeToString(hash);
                
                return calculatedSignature.equals(expectedSignature);
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("验证WhatsApp签名失败", e);
            return false;
        }
    }

    /**
     * 解析WhatsApp消息
     */
    public Map<String, Object> parseMessage(String body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> whatsappMessage = objectMapper.readValue(body, Map.class);
            log.info("解析WhatsApp消息: entry数量={}", 
                    whatsappMessage.containsKey("entry") ? 
                    ((java.util.List<?>) whatsappMessage.get("entry")).size() : 0);
            return whatsappMessage;
        } catch (Exception e) {
            log.error("解析WhatsApp消息失败", e);
            throw new RuntimeException("解析WhatsApp消息失败", e);
        }
    }

    /**
     * 将WhatsApp消息转换为统一的WebhookMessageRequest格式
     */
    public WebhookMessageRequest toWebhookRequest(Map<String, Object> whatsappMessage) {
        // WhatsApp消息格式：{"entry": [{"changes": [{"value": {"messages": [{"from": "xxx", "text": {"body": "xxx"}}]}}]}]}
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries = 
                (java.util.List<Map<String, Object>>) whatsappMessage.get("entry");
        
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("WhatsApp消息中没有entry");
        }
        
        Map<String, Object> entry = entries.get(0);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> changes = 
                (java.util.List<Map<String, Object>>) entry.get("changes");
        
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("WhatsApp消息中没有changes");
        }
        
        Map<String, Object> change = changes.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> value = (Map<String, Object>) change.get("value");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> messages = 
                (java.util.List<Map<String, Object>>) value.get("messages");
        
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("WhatsApp消息中没有messages");
        }
        
        Map<String, Object> message = messages.get(0);
        String from = (String) message.get("from");
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) message.get("text");
        String textBody = text != null ? (String) text.get("body") : "";
        
        return new WebhookMessageRequest(
                from,    // threadId（WhatsApp使用from作为threadId）
                textBody, // content
                "text",  // messageType
                from,    // externalUserId
                from,    // userName（需要从用户信息获取）
                null,    // email
                null,    // phone
                null,    // categoryId
                null,    // attachmentUrl
                null,    // attachmentName
                System.currentTimeMillis(), // timestamp
                null,    // language
                whatsappMessage // metadata
        );
    }

    /**
     * 发送消息到WhatsApp Business（通过WhatsApp官方SDK）
     * 
     * @param config 配置
     * @param phoneNumber 电话号码
     * @param content 消息内容
     * @param attachments 附件列表（可选）
     */
    public void sendMessage(OfficialChannelConfig config, String phoneNumber, String content,
                            List<Attachment> attachments) {
        // TODO: 使用WhatsApp SDK发送消息
        // 1. 从configJson中获取phoneNumberId、accessToken
        // 2. 调用WhatsApp API发送消息
        // POST https://graph.facebook.com/v18.0/{phone-number-id}/messages
        
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String phoneNumberId = (String) configData.get("phoneNumberId");
        String accessToken = (String) configData.get("accessToken");
        
        log.info("发送消息到WhatsApp Business: phoneNumberId={}, phoneNumber={}, content={}", 
                phoneNumberId, phoneNumber, content.length() > 50 ? content.substring(0, 50) + "..." : content);
        
        // TODO: 实现WhatsApp消息发送逻辑
        // 需要使用WhatsApp SDK或直接调用WhatsApp API
    }
}

