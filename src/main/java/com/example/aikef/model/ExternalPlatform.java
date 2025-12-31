package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 第三方平台配置
 * 用于配置 Line、WhatsApp、微信等外部平台的 webhook 回调
 */
@Entity
@Table(name = "external_platforms")
@Getter
@Setter
public class ExternalPlatform extends AuditableEntity {

    /**
     * 平台名称（唯一标识）
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 平台显示名称
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 平台类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false, length = 20)
    private PlatformType platformType;

    /**
     * 消息回调 URL（用于将客服/AI 消息转发到第三方平台）
     */
    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    /**
     * 回调认证类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", length = 20)
    private AuthType authType = AuthType.NONE;

    /**
     * 认证凭据（API Key、Token 等）
     */
    @Column(name = "auth_credential", length = 500)
    private String authCredential;

    /**
     * 额外的请求头（JSON 格式）
     */
    @Column(name = "extra_headers", columnDefinition = "TEXT")
    private String extraHeaders;

    /**
     * Webhook 密钥（用于验证来自第三方平台的请求）
     */
    @Column(name = "webhook_secret", length = 200)
    private String webhookSecret;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    /**
     * 平台类型枚举
     */
    public enum PlatformType {
        LINE,       // Line
        WHATSAPP,   // WhatsApp
        WECHAT,     // 微信
        TELEGRAM,   // Telegram
        FACEBOOK,   // Facebook Messenger
        TWITTER,    // X (Twitter)
        EMAIL,      // 邮件
        WEB,
        CUSTOM,     // 自定义平台
        OTHER       // 其他
    }

    /**
     * 认证类型枚举
     */
    public enum AuthType {
        NONE,           // 无认证
        API_KEY,        // API Key（Header: X-API-Key）
        BEARER_TOKEN,   // Bearer Token
        BASIC_AUTH,     // Basic Auth
        CUSTOM_HEADER   // 自定义 Header
    }
}

