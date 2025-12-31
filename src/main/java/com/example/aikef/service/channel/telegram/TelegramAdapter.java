package com.example.aikef.service.channel.telegram;

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
public class TelegramAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析Telegram消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> update) {
        try {
            Map<String, Object> message = (Map<String, Object>) update.get("message");
            if (message == null) return null;

            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            Map<String, Object> from = (Map<String, Object>) message.get("from");
            String text = (String) message.get("text");

            if (chat == null || from == null) return null;

            String chatId = String.valueOf(chat.get("id"));
            String fromId = String.valueOf(from.get("id"));
            String username = (String) from.get("username");

            return new WebhookMessageRequest(
                    chatId, // 使用 chatId 作为 threadId
                    text != null ? text : "[非文本消息]",
                    "text",
                    chatId, // 使用 chatId 作为 externalUserId (对于私聊是一样的)
                    username != null ? username : "Telegram User",
                    null,
                    null,
                    null,
                    null,
                    null,
                    System.currentTimeMillis(),
                    null,
                    Map.of("original_update", update)
            );
        } catch (Exception e) {
            log.error("转换Telegram消息失败", e);
            return null;
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(OfficialChannelConfig config, String chatId, String content, List<Attachment> attachments) {
        try {
            String botToken = getBotToken(config);
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> payload = Map.of(
                    "chat_id", chatId,
                    "text", content
            );
            
            // TODO: 支持 sendPhoto, sendDocument 等

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.postForObject(url, new HttpEntity<>(payload, headers), String.class);
            log.info("Telegram 消息发送成功: chatId={}", chatId);

        } catch (Exception e) {
            log.error("Telegram 消息发送失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    private String getBotToken(OfficialChannelConfig config) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config.getConfigJson(), Map.class);
            return (String) configMap.get("botToken");
        } catch (Exception e) {
            throw new RuntimeException("解析配置失败", e);
        }
    }
}
