package com.example.aikef.tool.internal.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.aikef.model.CustomerEmailConfig;
import com.example.aikef.repository.CustomerEmailConfigRepository;
import com.example.aikef.service.channel.email.EmailAdapter;
import com.example.aikef.tool.annotation.AutoInjectTool;
import com.example.aikef.workflow.context.WorkflowContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@AutoInjectTool
public class EmailTool {

    private final CustomerEmailConfigRepository configRepository;

    public EmailTool(CustomerEmailConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Tool("Configure the email account settings for the AI Agent. This is required before sending or receiving emails. System will also automatically check for new emails every minute.")
    public String configureEmail(
            @P(value = "Email address (e.g. user@example.com)", required = true) String email,
            @P(value = "Email password or App Password", required = true) String password,
            @P(value = "SMTP Host (e.g. smtp.gmail.com)", required = true) String smtpHost,
            @P(value = "SMTP Port (e.g. 587)", required = true) Integer smtpPort,
            @P(value = "IMAP Host (e.g. imap.gmail.com)", required = true) String imapHost,
            @P(value = "IMAP Port (e.g. 993)", required = true) Integer imapPort,
            WorkflowContext context
    ) {
        if (context == null || context.getCustomerId() == null) {
            return "Error: Customer context not found. Cannot configure email without a valid customer.";
        }
        UUID customerId = context.getCustomerId();

        try {
            CustomerEmailConfig config = configRepository.findByCustomerIdAndEmail(customerId, email)
                    .orElse(new CustomerEmailConfig());
            
            config.setCustomerId(customerId);
            config.setEmail(email);
            config.setEnabled(true);
            
            EmailAdapter.EmailConfig emailConfig = new EmailAdapter.EmailConfig();
            emailConfig.setEmail(email);
            emailConfig.setPassword(password);
            emailConfig.setSmtpHost(smtpHost);
            emailConfig.setSmtpPort(smtpPort);
            emailConfig.setImapHost(imapHost);
            emailConfig.setImapPort(imapPort);
            
            config.setConfigJson(JSONUtil.toJsonStr(emailConfig));
            configRepository.save(config);
            
            return "Email configuration saved successfully for " + email + ". Automatic monitoring is active.";
        } catch (Exception e) {
            log.error("Failed to save email config", e);
            return "Error saving email configuration: " + e.getMessage();
        }
    }

    @Tool("Receive recent emails from the configured inbox. Note: The system automatically monitors and processes new emails (and marks them as seen), so this tool is for manual checking/peeking only.")
    public String receiveEmails(
            @P(value = "Number of emails to fetch (default 5, max 20)") Integer limit,
            @P(value = "Fetch only unread emails (default false)") Boolean unreadOnly,
            WorkflowContext context
    ) {
        if (context == null || context.getCustomerId() == null) {
            return "Error: Customer context not found.";
        }
        UUID customerId = context.getCustomerId();

        limit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        unreadOnly = (unreadOnly != null) && unreadOnly;

        try {
            EmailAdapter.EmailConfig config = getEmailConfig(customerId);
            if (config == null) return "Error: Email not configured for this customer. Please use 'configureEmail' tool first.";

            Properties props = new Properties();
            props.put("mail.store.protocol", "imap");
            props.put("mail.imap.host", config.getImapHost());
            props.put("mail.imap.port", config.getImapPort());
            props.put("mail.imap.ssl.enable", "true");

            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            store.connect(config.getImapHost(), config.getEmail(), config.getPassword());

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages;
            if (unreadOnly) {
                messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            } else {
                int total = inbox.getMessageCount();
                int start = Math.max(1, total - limit + 1);
                messages = inbox.getMessages(start, total);
            }

            // Reverse to get newest first
            List<Message> messageList = Arrays.asList(messages);
            Collections.reverse(messageList);
            
            if (messageList.size() > limit) {
                messageList = messageList.subList(0, limit);
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (Message msg : messageList) {
                Map<String, String> emailData = new HashMap<>();
                emailData.put("subject", msg.getSubject());
                emailData.put("from", Arrays.toString(msg.getFrom()));
                emailData.put("date", msg.getSentDate().toString());
                emailData.put("content", getTextFromMessage(msg));
                result.add(emailData);
            }

            inbox.close(false);
            store.close();

            if (result.isEmpty()) return "No emails found.";
            return JSONUtil.toJsonStr(result);

        } catch (Exception e) {
            log.error("Failed to receive emails", e);
            return "Error receiving emails: " + e.getMessage();
        }
    }

    @Tool("Send an email to a recipient.")
    public String sendEmail(
            @P(value = "Recipient email address", required = true) String to,
            @P(value = "Email subject", required = true) String subject,
            @P(value = "Email body content", required = true) String body,
            WorkflowContext context
    ) {
        if (context == null || context.getCustomerId() == null) {
            return "Error: Customer context not found.";
        }
        UUID customerId = context.getCustomerId();

        try {
            EmailAdapter.EmailConfig config = getEmailConfig(customerId);
            if (config == null) return "Error: Email not configured for this customer. Please use 'configureEmail' tool first.";

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(config.getSmtpHost());
            mailSender.setPort(config.getSmtpPort());
            mailSender.setUsername(config.getEmail());
            mailSender.setPassword(config.getPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            // props.put("mail.debug", "true");

            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress(config.getEmail()));
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject(subject);
            message.setText(body, "UTF-8");

            mailSender.send(message);
            return "Email sent successfully to " + to;

        } catch (Exception e) {
            log.error("Failed to send email", e);
            return "Error sending email: " + e.getMessage();
        }
    }

    private EmailAdapter.EmailConfig getEmailConfig(UUID customerId) {
        // Return the first configured email for this customer (or enhance to select by email address if needed)
        List<CustomerEmailConfig> configs = configRepository.findByCustomerId(customerId);
        if (CollUtil.isEmpty(configs)) return null;
        
        // Use the first one for now, or maybe the most recently updated?
        CustomerEmailConfig config = configs.get(0);
        return JSONUtil.toBean(config.getConfigJson(), EmailAdapter.EmailConfig.class);
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
}