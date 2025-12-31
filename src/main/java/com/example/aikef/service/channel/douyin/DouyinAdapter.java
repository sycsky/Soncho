package com.example.aikef.service.channel.douyin;

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
public class DouyinAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析抖音消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> message) {
        return null; // 占位
    }

    public void sendMessage(OfficialChannelConfig config, String openId, String content, List<Attachment> attachments) {
        log.info("抖音消息发送: openId={}, content={}", openId, content);
        // 实现抖音私信发送逻辑
        // POST https://open.douyin.com/im/send/msg/
    }

    public boolean verifySignature(OfficialChannelConfig config, String signature, String timestamp, String nonce, String body) {
        // 实现抖音验签逻辑
        return true; 
    }
}
