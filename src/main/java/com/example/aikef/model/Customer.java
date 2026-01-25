package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Entity
@Table(name = "customers")
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

    @Column(name = "shopify_customer_id", length = 50)
    private String shopifyCustomerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", name = "shopify_customer_info")
    private Map<String, Object> shopifyCustomerInfo = new HashMap<>();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Channel getPrimaryChannel() {
        return primaryChannel;
    }

    public void setPrimaryChannel(Channel primaryChannel) {
        this.primaryChannel = primaryChannel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWechatOpenId() {
        return wechatOpenId;
    }

    public void setWechatOpenId(String wechatOpenId) {
        this.wechatOpenId = wechatOpenId;
    }

    public String getWhatsappId() {
        return whatsappId;
    }

    public void setWhatsappId(String whatsappId) {
        this.whatsappId = whatsappId;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public java.time.Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(java.time.Instant lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getAiTags() {
        return aiTags;
    }

    public void setAiTags(List<String> aiTags) {
        this.aiTags = aiTags;
    }

    public String getShopifyCustomerId() {
        return shopifyCustomerId;
    }

    public void setShopifyCustomerId(String shopifyCustomerId) {
        this.shopifyCustomerId = shopifyCustomerId;
    }

    public Map<String, Object> getShopifyCustomerInfo() {
        return shopifyCustomerInfo;
    }

    public void setShopifyCustomerInfo(Map<String, Object> shopifyCustomerInfo) {
        this.shopifyCustomerInfo = shopifyCustomerInfo;
    }
}
