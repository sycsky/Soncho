package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 官方渠道配置
 * 用于配置微信服务号、Line、WhatsApp等官方渠道的密钥和配置信息
 * 每个平台只有一个配置记录（存在就更新，不存在就创建）
 */
@Entity
@Table(name = "official_channel_configs", 
       uniqueConstraints = @UniqueConstraint(columnNames = "channel_type"))
@Getter
@Setter
public class OfficialChannelConfig extends AuditableEntity {

    /**
     * 渠道类型（唯一标识）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, unique = true, length = 20, columnDefinition = "VARCHAR(20)")
    private ChannelType channelType;

    /**
     * 渠道显示名称
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /**
     * 配置信息（JSON格式，存储各平台特定的配置）
     * 例如：
     * - 微信：appId, appSecret, token, encodingAESKey
     * - Line：channelId, channelSecret, channelAccessToken
     * - WhatsApp：phoneNumberId, accessToken, businessAccountId
     */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    /**
     * Webhook验证密钥（用于验证来自官方平台的请求）
     */
    @Column(name = "webhook_secret", length = 200)
    private String webhookSecret;

    /**
     * Webhook URL（系统提供的固定URL，用于接收官方平台消息）
     * 格式：/api/v1/official-channels/{channelType}/webhook
     */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    /**
     * 会话分类ID（可选）
     * 当收到官方渠道消息创建会话时，如果配置了此字段，会自动设置会话分类
     */
    @Column(name = "category_id", length = 36)
    private java.util.UUID categoryId;

    /**
     * 渠道类型枚举
     */
    public enum ChannelType {
        WECHAT_OFFICIAL,  // 微信服务号
        WECHAT_KF,        // 微信客服
        LINE_OFFICIAL,    // Line官方账号
        WHATSAPP_OFFICIAL, // WhatsApp Business
        FACEBOOK_MESSENGER, // Facebook Messenger
        INSTAGRAM,        // Instagram
        TELEGRAM,         // Telegram
        TWITTER,          // X (Twitter)
        DOUYIN,           // 抖音
        RED_BOOK,         // 小红书
        WEIBO,            // 微博
        EMAIL             // 邮件
    }
}
