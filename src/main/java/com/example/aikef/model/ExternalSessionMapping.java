package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * 外部会话映射
 * 将第三方平台的 threadId 与系统内部的会话 ID 进行映射
 */
@Entity
@Table(name = "external_session_mappings", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"platform_id", "external_thread_id"}, name = "uk_platform_thread")
       },
       indexes = {
           @Index(columnList = "external_thread_id", name = "idx_external_thread_id"),
           @Index(columnList = "session_id", name = "idx_session_id")
       })
@Getter
@Setter
public class ExternalSessionMapping extends AuditableEntity {

    /**
     * 关联的第三方平台
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private ExternalPlatform platform;

    /**
     * 第三方平台的会话/线程 ID
     */
    @Column(name = "external_thread_id", nullable = false, length = 200)
    private String externalThreadId;

    /**
     * 系统内部的会话
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    /**
     * 关联的客户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * 外部用户 ID（如微信 openId、Line userId）
     */
    @Column(name = "external_user_id", length = 200)
    private String externalUserId;

    /**
     * 外部用户名称
     */
    @Column(name = "external_user_name", length = 200)
    private String externalUserName;

    /**
     * 额外元数据（JSON 格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 是否活跃
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}

