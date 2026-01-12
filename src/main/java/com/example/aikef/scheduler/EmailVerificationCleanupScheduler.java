package com.example.aikef.scheduler;

import com.example.aikef.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码清理定时任务
 * 定期清理过期和已使用的验证码
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationCleanupScheduler {

    private final EmailVerificationService emailVerificationService;

    /**
     * 每小时清理一次过期的验证码
     * cron 表达式: 每小时的第0分钟执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredCodes() {
        log.info("开始清理过期的邮箱验证码...");
        try {
            emailVerificationService.cleanupExpiredCodes();
            log.info("邮箱验证码清理完成");
        } catch (Exception e) {
            log.error("清理邮箱验证码失败", e);
        }
    }
}

