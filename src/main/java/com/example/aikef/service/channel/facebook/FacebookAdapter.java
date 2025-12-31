package com.example.aikef.service.channel.facebook;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.OfficialChannelConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacebookAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 验证 Webhook (Hub Challenge)
     */
    public boolean verifyWebhook(OfficialChannelConfig config, String mode, String token, String challenge) {
        if (config == null || config.getWebhookSecret() == null) {
            return false;
        }
        return "subscribe".equals(mode) && config.getWebhookSecret().equals(token);
    }

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析Facebook消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> message) {
        // 简化处理：只提取第一个 entry 的第一个 messaging
        try {
            List<Map<String, Object>> entryList = (List<Map<String, Object>>) message.get("entry");
            if (entryList == null || entryList.isEmpty()) return null;

            Map<String, Object> entry = entryList.get(0);
            List<Map<String, Object>> messagingList = (List<Map<String, Object>>) entry.get("messaging");
            if (messagingList == null || messagingList.isEmpty()) return null;

            Map<String, Object> messaging = messagingList.get(0);
            Map<String, Object> sender = (Map<String, Object>) messaging.get("sender");
            Map<String, Object> msg = (Map<String, Object>) messaging.get("message");

            if (sender == null || msg == null) return null;

            String senderId = (String) sender.get("id");
            String content = (String) msg.get("text");
            // TODO: 处理附件
            
            return new WebhookMessageRequest(
                    senderId,
                    content,
                    "text",
                    senderId,
                    "Facebook User",
                    null,
                    null,
                    null,
                    null,
                    null,
                    System.currentTimeMillis(),
                    null,
                    Map.of("original_message", messaging)
            );
        } catch (Exception e) {
            log.error("转换Facebook消息失败", e);
            return null;
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(OfficialChannelConfig config, String recipientId, String content, List<Attachment> attachments) {
        try {
            String pageAccessToken = getPageAccessToken(config);
            String url = "https://graph.facebook.com/v18.0/me/messages?access_token=" + pageAccessToken;

            Map<String, Object> recipient = Map.of("id", recipientId);
            Map<String, Object> message = Map.of("text", content);

            // TODO: 处理附件

            Map<String, Object> payload = Map.of(
                    "recipient", recipient,
                    "message", message,
                    "messaging_type", "RESPONSE"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForObject(url, new HttpEntity<>(payload, headers), String.class);
            log.info("Facebook/Instagram 消息发送成功: recipientId={}", recipientId);

        } catch (Exception e) {
            log.error("Facebook/Instagram 消息发送失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    private String getPageAccessToken(OfficialChannelConfig config) {
        // 从 configJson 中获取 accessToken
        try {
            Map<String, Object> configMap = objectMapper.readValue(config.getConfigJson(), Map.class);
            return (String) configMap.get("accessToken");
        } catch (Exception e) {
            throw new RuntimeException("解析配置失败", e);
        }
    }
}
