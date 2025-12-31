package com.example.aikef.service.channel.weibo;

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
public class WeiboAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析微博消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> message) {
        return null; // 占位
    }

    public void sendMessage(OfficialChannelConfig config, String receiverId, String content, List<Attachment> attachments) {
        log.info("微博消息发送: receiverId={}, content={}", receiverId, content);
        // 实现微博粉丝服务平台消息发送
        // https://api.weibo.com/2/messages/send.json
    }
    
    public boolean verifySignature(OfficialChannelConfig config, String signature, String timestamp, String nonce) {
        // 微博验签
        return true;
    }
}
