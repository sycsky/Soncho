package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 邮箱验证码实体
 */
@Entity
@Table(name = "email_verification_codes")
@Getter
@Setter
public class EmailVerificationCode extends AuditableEntity {

    /**
     * 邮箱地址
     */
    @Column(nullable = false)
    private String email;

    /**
     * 验证码
     */
    @Column(nullable = false, length = 6)
    private String code;

    /**
     * 关联的客户ID（可选）
     */
    @Column(name = "customer_id")
    private UUID customerId;

    /**
     * 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 是否已使用
     */
    @Column(nullable = false)
    private boolean used = false;

    /**
     * 使用时间
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * 发送次数（同一邮箱的重试次数）
     */
    @Column(name = "send_count", nullable = false)
    private int sendCount = 1;

    /**
     * 验证码类型（绑定邮箱、重置密码等）
     */
    @Column(name = "verification_type", length = 50)
    private String verificationType;
}

