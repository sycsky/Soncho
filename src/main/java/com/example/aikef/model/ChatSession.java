package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.model.enums.SessionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_sessions")
@Data
public class ChatSession extends AuditableEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.AI_HANDLING;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt = Instant.now();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_agent_id")
    private Agent primaryAgent;

    /**
     * 会话分类（可选）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private SessionCategory category;

    @ElementCollection
    @CollectionTable(name = "chat_session_support_agents", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "agent_id")
    private List<UUID> supportAgentIds = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String note;

    /**
     * 客户使用的语言代码（如 zh-TW, en, ja）
     * WEB端在创建时由前端传输，其他渠道从webhook消息中获取
     * 如果没有提供，会自动检测第一条用户消息的语言
     */
    @Column(name = "customer_language", length = 10)
    private String customerLanguage;

    /**
     * 会话元数据（JSON 格式）
     * 存储自定义参数，如来源、设备信息等
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;


    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }


    public Agent getPrimaryAgent() {
        return primaryAgent;
    }

    public void setPrimaryAgent(Agent primaryAgent) {
        this.primaryAgent = primaryAgent;
    }

    public List<UUID> getSupportAgentIds() {
        return supportAgentIds;
    }

    public void setSupportAgentIds(List<UUID> supportAgentIds) {
        this.supportAgentIds = supportAgentIds;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public SessionCategory getCategory() {
        return category;
    }

    public void setCategory(SessionCategory category) {
        this.category = category;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getCustomerLanguage() {
        return customerLanguage;
    }

    public void setCustomerLanguage(String customerLanguage) {
        this.customerLanguage = customerLanguage;
    }
}
