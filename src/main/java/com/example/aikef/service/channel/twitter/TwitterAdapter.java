package com.example.aikef.service.channel.twitter;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.OfficialChannelConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TwitterAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析Twitter消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> message) {
        // Twitter Account Activity API 格式复杂，这里仅作占位
        return null;
    }

    /**
     * 验证 CRC (Challenge-Response Check)
     */
    public String generateCrcResponse(OfficialChannelConfig config, String crcToken) {
        try {
            String consumerSecret = getConsumerSecret(config);
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(consumerSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(crcToken.getBytes(StandardCharsets.UTF_8)));
            return "sha256=" + hash;
        } catch (Exception e) {
            log.error("生成 CRC 响应失败", e);
            return null;
        }
    }

    /**
     * 发送消息 (Direct Message)
     */
    public void sendMessage(OfficialChannelConfig config, String recipientId, String content, List<Attachment> attachments) {
        // Twitter API v2 DM implementation
        log.info("Twitter 消息发送暂未实现完全对接，需处理 OAuth 1.0a 或 OAuth 2.0 User Context");
        // 这里只是占位，实际需要复杂的 OAuth 签名
    }

    private String getConsumerSecret(OfficialChannelConfig config) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(config.getConfigJson(), Map.class);
            return (String) configMap.get("consumerSecret");
        } catch (Exception e) {
            throw new RuntimeException("解析配置失败", e);
        }
    }
}
