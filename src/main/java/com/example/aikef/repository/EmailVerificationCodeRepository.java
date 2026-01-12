package com.example.aikef.repository;

import com.example.aikef.model.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    /**
     * 查找未使用且未过期的验证码
     */
    Optional<EmailVerificationCode> findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(
            String email, String code, Instant now);

    /**
     * 查找邮箱的最新验证码
     */
    Optional<EmailVerificationCode> findFirstByEmailOrderByCreatedAtDesc(String email);

    /**
     * 统计指定时间内发送的验证码数量
     */
    long countByEmailAndCreatedAtAfter(String email, Instant since);

    /**
     * 删除过期的验证码
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationCode e WHERE e.expiresAt < :now OR (e.used = true AND e.usedAt < :cleanupTime)")
    int deleteExpiredOrUsedCodes(Instant now, Instant cleanupTime);
}

