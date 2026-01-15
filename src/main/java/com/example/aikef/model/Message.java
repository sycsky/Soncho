package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.model.enums.SenderType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "messages")
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)"))
})
public class Message extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(columnDefinition = "text")
    private String text;

    @Column(name = "is_internal", nullable = false)
    private boolean internal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agent_metadata", columnDefinition = "json")
    private Map<String, Object> agentMetadata = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "translation_data", columnDefinition = "json")
    private Map<String, Object> translationData = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_call_data", columnDefinition = "json")
    private Map<String, Object> toolCallData = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "message_mentions", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "mention_id")
    private List<String> mentionAgentIds = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @Column(name = "read_by_customer", nullable = false)
    private boolean readByCustomer = false;

    @Column(name = "read_by_agent", nullable = false)
    private boolean readByAgent = false;

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public SenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(SenderType senderType) {
        this.senderType = senderType;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public Map<String, Object> getTranslationData() {
        return translationData;
    }

    public void setTranslationData(Map<String, Object> translationData) {
        this.translationData = translationData;
    }

    public List<String> getMentionAgentIds() {
        return mentionAgentIds;
    }

    public void setMentionAgentIds(List<String> mentionAgentIds) {
        this.mentionAgentIds = mentionAgentIds;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public Map<String, Object> getAgentMetadata() {
        return agentMetadata;
    }

    public void setAgentMetadata(Map<String, Object> agentMetadata) {
        this.agentMetadata = agentMetadata;
    }

    public boolean isReadByCustomer() {
        return readByCustomer;
    }

    public void setReadByCustomer(boolean readByCustomer) {
        this.readByCustomer = readByCustomer;
    }

    public boolean isReadByAgent() {
        return readByAgent;
    }

    public void setReadByAgent(boolean readByAgent) {
        this.readByAgent = readByAgent;
    }

    public Map<String, Object> getToolCallData() {
        return toolCallData;
    }

    public void setToolCallData(Map<String, Object> toolCallData) {
        this.toolCallData = toolCallData;
    }
}
