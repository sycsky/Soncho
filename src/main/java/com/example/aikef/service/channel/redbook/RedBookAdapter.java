package com.example.aikef.service.channel.redbook;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.OfficialChannelConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedBookAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析小红书消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> message) {
        return null; // 占位
    }

    public void sendMessage(OfficialChannelConfig config, String userId, String content, List<Attachment> attachments) {
        log.info("小红书消息发送: userId={}, content={}", userId, content);
        // 实现小红书私信发送逻辑
    }
    
    public boolean verifySignature(OfficialChannelConfig config, String signature, String body) {
        // 实现小红书验签
        return true;
    }
}
