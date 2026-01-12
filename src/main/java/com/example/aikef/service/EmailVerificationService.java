package com.example.aikef.service;

import com.example.aikef.model.Customer;
import com.example.aikef.model.EmailVerificationCode;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * 邮箱验证码服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final CustomerRepository customerRepository;
    private final ResendEmailService resendEmailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${email.verification.code.length:6}")
    private int codeLength;

    @Value("${email.verification.code.expiry.minutes:10}")
    private int expiryMinutes;

    @Value("${email.verification.max.attempts.per.hour:5}")
    private int maxAttemptsPerHour;

    /**
     * 生成并发送验证码
     *
     * @param email      邮箱地址
     * @param customerId 客户ID（可选）
     * @return 发送结果消息
     */
    @Transactional
    public String sendVerificationCode(String email, UUID customerId) {
        // 验证邮箱格式
        if (!isValidEmail(email)) {
            return "Invalid email format / 邮箱格式不正确";
        }

        // 检查发送频率限制
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentCount = verificationCodeRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        if (recentCount >= maxAttemptsPerHour) {
            return "Too many attempts. Please try again later / 发送次数过多，请稍后再试";
        }

        // 检查是否在短时间内已发送过验证码
        Optional<EmailVerificationCode> recentCode = verificationCodeRepository
                .findFirstByEmailOrderByCreatedAtDesc(email);
        if (recentCode.isPresent()) {
            Instant lastSent = recentCode.get().getCreatedAt();
            if (lastSent.isAfter(Instant.now().minus(1, ChronoUnit.MINUTES))) {
                return "Please wait before requesting another code / 请等待后再次发送验证码";
            }
        }

        // 生成验证码
        String code = generateVerificationCode();

        // 创建验证码记录
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setCustomerId(customerId);
        verificationCode.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        verificationCode.setUsed(false);
        verificationCode.setVerificationType("EMAIL_BINDING");

        try {
            // 发送邮件
            boolean sent = resendEmailService.sendVerificationCode(email, code);
            if (!sent) {
                return "Failed to send verification email / 验证邮件发送失败";
            }

            // 保存验证码记录
            verificationCodeRepository.save(verificationCode);

            log.info("验证码已发送: email={}, customerId={}", email, customerId);
            return "Verification code sent successfully / 验证码已发送";

        } catch (Exception e) {
            log.error("发送验证码失败: email={}", email, e);
            return "Failed to send verification code / 验证码发送失败";
        }
    }

    /**
     * 验证验证码并绑定邮箱到客户
     *
     * @param email      邮箱地址
     * @param code       验证码
     * @param customerId 客户ID
     * @return 验证结果消息
     */
    @Transactional
    public String verifyAndBindEmail(String email, String code, UUID customerId) {
        // 验证参数
        if (email == null || email.isBlank()) {
            return "Email is required / 邮箱地址不能为空";
        }
        if (code == null || code.isBlank()) {
            return "Verification code is required / 验证码不能为空";
        }
        if (customerId == null) {
            return "Customer ID is required / 客户ID不能为空";
        }

        // 查找客户
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty()) {
            return "Customer not found / 客户不存在";
        }
        Customer customer = customerOpt.get();

        // 检查邮箱是否已被其他客户使用
        Optional<Customer> existingCustomer = customerRepository.findByEmail(email);
        if (existingCustomer.isPresent() && !existingCustomer.get().getId().equals(customerId)) {
            return "Email is already bound to another customer / 该邮箱已被其他客户绑定";
        }

        // 查找验证码
        Optional<EmailVerificationCode> verificationCodeOpt = verificationCodeRepository
                .findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(email, code, Instant.now());

        if (verificationCodeOpt.isEmpty()) {
            return "Invalid or expired verification code / 验证码无效或已过期";
        }

        EmailVerificationCode verificationCode = verificationCodeOpt.get();

        try {
            // 标记验证码为已使用
            verificationCode.setUsed(true);
            verificationCode.setUsedAt(Instant.now());
            verificationCodeRepository.save(verificationCode);

            // 绑定邮箱到客户
            customer.setEmail(email);
            customerRepository.save(customer);

            log.info("邮箱绑定成功: customerId={}, email={}", customerId, email);
            return "Email verified and bound successfully / 邮箱验证并绑定成功";

        } catch (Exception e) {
            log.error("邮箱绑定失败: customerId={}, email={}", customerId, email, e);
            return "Failed to bind email / 邮箱绑定失败";
        }
    }

    /**
     * 验证验证码（不绑定邮箱）
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否验证成功
     */
    @Transactional
    public boolean verifyCode(String email, String code) {
        Optional<EmailVerificationCode> verificationCodeOpt = verificationCodeRepository
                .findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(email, code, Instant.now());

        if (verificationCodeOpt.isEmpty()) {
            return false;
        }

        EmailVerificationCode verificationCode = verificationCodeOpt.get();
        verificationCode.setUsed(true);
        verificationCode.setUsedAt(Instant.now());
        verificationCodeRepository.save(verificationCode);

        return true;
    }

    /**
     * 清理过期的验证码
     */
    @Transactional
    public void cleanupExpiredCodes() {
        Instant now = Instant.now();
        Instant cleanupTime = now.minus(7, ChronoUnit.DAYS);
        int deleted = verificationCodeRepository.deleteExpiredOrUsedCodes(now, cleanupTime);
        log.info("清理过期验证码: 删除{}条记录", deleted);
    }

    /**
     * 生成随机验证码
     */
    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}

