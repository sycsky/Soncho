package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Entity
@Table(name = "customers")
@Data
public class Customer extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel primaryChannel;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "wechat_openid", unique = true)
    private String wechatOpenId;

    @Column(name = "whatsapp_id", unique = true)
    private String whatsappId;

    @Column(name = "line_id", unique = true)
    private String lineId;

    @Column(name = "telegram_id", unique = true)
    private String telegramId;

    @Column(name = "facebook_id", unique = true)
    private String facebookId;

    private String avatarUrl;

    private String location;

    @Column(length = 1000)
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", name = "custom_fields")
    private Map<String, Object> customFields = new HashMap<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_interaction_at")
    private java.time.Instant lastInteractionAt;

    // 手动添加的标签（客服手动添加）
    @ElementCollection
    @CollectionTable(name = "user_tags", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    // AI生成的标签（AI自动添加）
    @ElementCollection
    @CollectionTable(name = "user_ai_tags", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "tag")
    private List<String> aiTags = new ArrayList<>();
}
