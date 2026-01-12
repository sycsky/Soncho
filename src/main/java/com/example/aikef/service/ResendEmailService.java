package com.example.aikef.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resend 邮件发送服务
 * 使用 Resend API 发送邮件验证码
 * 
 * 官方文档: https://resend.com/docs/api-reference/emails/send-email
 */
@Service
@Slf4j
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.from.email:noreply@yourdomain.com}")
    private String fromEmail;

    @Value("${resend.from.name:Customer Service}")
    private String fromName;

    private final RestTemplate restTemplate;

    public ResendEmailService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 发送验证码邮件
     *
     * @param toEmail 收件人邮箱
     * @param code    验证码
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String toEmail, String code) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Resend API Key 未配置，无法发送邮件");
            return false;
        }

        try {
            // 构建邮件内容
            String subject = "邮箱验证码 - Email Verification Code";
            String htmlContent = buildVerificationEmailHtml(code);
            String textContent = buildVerificationEmailText(code);

            // 发送邮件
            return sendEmail(toEmail, subject, htmlContent, textContent);

        } catch (Exception e) {
            log.error("发送验证码邮件失败: toEmail={}", toEmail, e);
            return false;
        }
    }

    /**
     * 发送邮件通用方法
     *
     * @param toEmail     收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML 内容
     * @param textContent 纯文本内容（备用）
     * @return 是否发送成功
     */
    public boolean sendEmail(String toEmail, String subject, String htmlContent, String textContent) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", fromName + " <" + fromEmail + ">");
            requestBody.put("to", List.of(toEmail));
            requestBody.put("subject", subject);
            requestBody.put("html", htmlContent);
            requestBody.put("text", textContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    RESEND_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("邮件发送成功: toEmail={}, subject={}", toEmail, subject);
                return true;
            } else {
                log.error("邮件发送失败: toEmail={}, status={}, response={}", 
                        toEmail, response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            log.error("发送邮件异常: toEmail={}, subject={}", toEmail, subject, e);
            return false;
        }
    }

    /**
     * 构建验证码邮件的 HTML 内容
     */
    private String buildVerificationEmailHtml(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4a90e2; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                        .code-box { background-color: #ffffff; border: 2px solid #4a90e2; padding: 20px; text-align: center; margin: 20px 0; border-radius: 5px; }
                        .code { font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #4a90e2; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #999; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>邮箱验证码</h1>
                            <p>Email Verification Code</p>
                        </div>
                        <div class="content">
                            <p>您好！</p>
                            <p>您正在进行邮箱验证，请使用以下验证码完成验证：</p>
                            <div class="code-box">
                                <div class="code">%s</div>
                            </div>
                            <p><strong>注意事项：</strong></p>
                            <ul>
                                <li>验证码有效期为 10 分钟</li>
                                <li>请勿将验证码透露给他人</li>
                                <li>如果这不是您的操作，请忽略此邮件</li>
                            </ul>
                            <hr>
                            <p>Hello!</p>
                            <p>You are verifying your email. Please use the following verification code:</p>
                            <p><strong>Notes:</strong></p>
                            <ul>
                                <li>The verification code is valid for 10 minutes</li>
                                <li>Do not share the verification code with others</li>
                                <li>If this was not your action, please ignore this email</li>
                            </ul>
                        </div>
                        <div class="footer">
                            <p>此邮件由系统自动发送，请勿回复</p>
                            <p>This is an automated email, please do not reply</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(code);
    }

    /**
     * 构建验证码邮件的纯文本内容
     */
    private String buildVerificationEmailText(String code) {
        return """
                邮箱验证码 / Email Verification Code
                
                您好！
                您正在进行邮箱验证，请使用以下验证码完成验证：
                
                验证码：%s
                
                注意事项：
                - 验证码有效期为 10 分钟
                - 请勿将验证码透露给他人
                - 如果这不是您的操作，请忽略此邮件
                
                ---
                
                Hello!
                You are verifying your email. Please use the following verification code:
                
                Verification Code: %s
                
                Notes:
                - The verification code is valid for 10 minutes
                - Do not share the verification code with others
                - If this was not your action, please ignore this email
                
                此邮件由系统自动发送，请勿回复
                This is an automated email, please do not reply
                """.formatted(code, code);
    }
}

