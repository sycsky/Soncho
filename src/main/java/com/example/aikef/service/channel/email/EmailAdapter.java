package com.example.aikef.service.channel.email;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.Attachment;
import com.example.aikef.model.Channel;
import com.example.aikef.model.CustomerEmailConfig;
import com.example.aikef.model.ExternalPlatform;
import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.repository.CustomerEmailConfigRepository;
import com.example.aikef.repository.ExternalPlatformRepository;
import com.example.aikef.repository.OfficialChannelConfigRepository;
import com.example.aikef.service.ExternalPlatformService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailAdapter {

    private final ObjectMapper objectMapper;
    private final OfficialChannelConfigRepository configRepository;
    private final CustomerEmailConfigRepository customerConfigRepository;
    private final ExternalPlatformService externalPlatformService;
    private final ExternalPlatformRepository platformRepository;

    // 定时检查邮件 (每分钟)
    @Scheduled(fixedDelay = 60000)
    public void monitorEmails() {
        ensureEmailPlatformExists();
        monitorSystemEmail();
        monitorCustomerEmails();
    }

    private void monitorSystemEmail() {
        OfficialChannelConfig configEntity = configRepository.findByChannelType(OfficialChannelConfig.ChannelType.EMAIL).orElse(null);
        if (configEntity == null) {
            return;
        }

        try {
            EmailConfig config = JSONUtil.toBean(configEntity.getConfigJson(), EmailConfig.class);
            if (StrUtil.isBlank(config.getImapHost())) return;

            processEmailAccount(config, null, configEntity.getCategoryId() != null ? configEntity.getCategoryId().toString() : null);

        } catch (Exception e) {
            log.error("Error in system email monitor task", e);
        }
    }

    private void monitorCustomerEmails() {
        List<CustomerEmailConfig> configs = customerConfigRepository.findByEnabledTrue();
        for (CustomerEmailConfig configEntity : configs) {
            try {
                EmailConfig config = JSONUtil.toBean(configEntity.getConfigJson(), EmailConfig.class);
                if (StrUtil.isBlank(config.getImapHost())) continue;

                processEmailAccount(config, configEntity.getCustomerId(), null);

                // Update last check time
                configEntity.setLastCheckTime(LocalDateTime.now());
                customerConfigRepository.save(configEntity);

            } catch (Exception e) {
                log.error("Error checking email for customer {}", configEntity.getCustomerId(), e);
            }
        }
    }

    private void processEmailAccount(EmailConfig config, UUID customerId, String defaultCategoryId) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", config.getImapHost());
        props.put("mail.imap.port", config.getImapPort());
        props.put("mail.imap.ssl.enable", "true");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            try {
                store.connect(config.getImapHost(), config.getEmail(), config.getPassword());
            } catch (Exception e) {
                log.warn("Failed to connect to email server for {}: {}", config.getEmail(), e.getMessage());
                return;
            }

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE); // 需要标记为已读

            // 只获取未读邮件
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message msg : messages) {
                try {
                    processSingleMessage(msg, customerId, defaultCategoryId);
                } catch (Exception e) {
                    log.error("Error processing email message", e);
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            log.error("Error processing email account: {}", config.getEmail(), e);
        }
    }

    private void processSingleMessage(Message msg, UUID customerId, String defaultCategoryId) throws MessagingException, IOException {
        String subject = msg.getSubject();
        String from = jakarta.mail.internet.InternetAddress.toString(msg.getFrom());
        String content = getTextFromMessage(msg);
        String messageId = getMessageId(msg);

        String senderEmail = extractEmail(from);
        String senderName = extractName(from);

        WebhookMessageRequest request = new WebhookMessageRequest(
                messageId,
                "Subject: " + subject + "\n\n" + content,
                "text",
                senderEmail,
                senderName,
                senderEmail,
                null,
                defaultCategoryId,
                null,
                null,
                msg.getSentDate().toInstant().toEpochMilli(),
                "en", // 默认语言，或通过检测
                Map.of("subject", subject, "source", "EMAIL")
        );

        if (customerId != null) {
            externalPlatformService.handleEmailMessage(customerId, request);
        } else {
            externalPlatformService.handleWebhookMessage("email", request);
        }

        // 标记为已读
        msg.setFlag(Flags.Flag.SEEN, true);
    }

    private void ensureEmailPlatformExists() {
        if (platformRepository.findByNameAndEnabledTrue("email").isEmpty()) {
            ExternalPlatform platform = new ExternalPlatform();
            platform.setName("email");
            platform.setDisplayName("Email Channel");
            platform.setPlatformType(Channel.EMAIL);
            platform.setEnabled(true);
            platform.setCallbackUrl("internal://email");
            platformRepository.save(platform);
            log.info("Created default 'email' ExternalPlatform");
        }
    }

    private String getMessageId(Message msg) {
        try {
            if (msg instanceof jakarta.mail.internet.MimeMessage) {
                return ((jakarta.mail.internet.MimeMessage) msg).getMessageID();
            }
        } catch (Exception e) {
            // ignore
        }
        return UUID.randomUUID().toString();
    }

    private String extractEmail(String from) {
        if (from == null) return "unknown@email.com";
        int start = from.lastIndexOf('<');
        int end = from.lastIndexOf('>');
        if (start >= 0 && end > start) {
            return from.substring(start + 1, end);
        }
        return from;
    }

    private String extractName(String from) {
        if (from == null) return "Unknown";
        int start = from.lastIndexOf('<');
        if (start > 0) {
            return from.substring(0, start).trim().replaceAll("\"", "");
        }
        return from;
    }

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
        try {
            EmailConfig emailConfig = JSONUtil.toBean(config.getConfigJson(), EmailConfig.class);
            
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", emailConfig.getSmtpHost());
            props.put("mail.smtp.port", emailConfig.getSmtpPort());

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailConfig.getEmail(), emailConfig.getPassword());
                }
            });

            jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(emailConfig.getEmail()));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(toEmail));
            message.setSubject("Re: Customer Support"); // 默认主题，实际应传参
            message.setText(content);

            Transport.send(message);
            log.info("Email sent successfully to {}", toEmail);

        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        try {
            if (message.isMimeType("text/plain")) {
                return message.getContent().toString();
            } 
            if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                return getTextFromMimeMultipart(mimeMultipart);
            }
        } catch (Exception e) {
            log.warn("Error parsing email content", e);
            return "[Error parsing content]";
        }
        return "[No text content]";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
                break; 
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(html.replaceAll("<[^>]*>", "")); 
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result.append(getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent()));
            }
        }
        return StrUtil.subPre(result.toString(), 500);
    }

    @Data
    public static class EmailConfig {
        private String email;
        private String password;
        private String smtpHost;
        private Integer smtpPort;
        private String imapHost;
        private Integer imapPort;
    }
}
