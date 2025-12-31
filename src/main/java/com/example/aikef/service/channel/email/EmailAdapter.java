package com.example.aikef.service.channel.email;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.OfficialChannelConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailAdapter {

    private final ObjectMapper objectMapper;

    // 如果引入了 spring-boot-starter-mail，可以注入 JavaMailSender
    // private final JavaMailSender javaMailSender;

    public Map<String, Object> parseMessage(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析邮件消息失败", e);
            return Map.of();
        }
    }

    public WebhookMessageRequest toWebhookRequest(Map<String, Object> message) {
        return null; // 占位
    }

    public void sendMessage(OfficialChannelConfig config, String toEmail, String content, List<Attachment> attachments) {
        log.info("邮件发送: to={}, content={}", toEmail, content);
        // 这里可以集成 SendGrid, Mailgun 或者 SMTP
        // 示例：使用 SMTP
        try {
            Map<String, Object> configMap = objectMapper.readValue(config.getConfigJson(), Map.class);
            String smtpHost = (String) configMap.get("smtpHost");
            // ... 实际发送逻辑
        } catch (Exception e) {
            log.error("邮件配置解析失败", e);
        }
    }
}
