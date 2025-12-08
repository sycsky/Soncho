package com.example.aikef.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 消息发送记录表
 * 用于追踪每条消息对每个客服的发送状态
 * 注意：只为客服创建发送记录，客户通过历史消息接口获取消息
 */
@Entity
@Table(name = "message_delivery",
        indexes = {
                @Index(name = "idx_agent_not_sent", columnList = "agent_id,is_sent"),
                @Index(name = "idx_message", columnList = "message_id")
        })
public class MessageDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 关联的消息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    /**
     * 接收客服ID
     */
    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    /**
     * 接收客户ID（预留字段，当前不使用）
     */
    @Column(name = "customer_id")
    private UUID customerId;

    /**
     * 是否已发送
     */
    @Column(name = "is_sent", nullable = false)
    private boolean isSent = false;

    /**
     * 发送时间
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
