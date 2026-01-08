package com.example.aikef.tool.model;

import com.example.aikef.extraction.model.ExtractionSchema;
import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AI 工具定义
 * 支持 MCP 服务和 API 接口两种类型
 * 每个工具关联一个 ExtractionSchema（1对1关系）
 */
@Data
@Entity
@Table(name = "ai_tools", indexes = {
        @Index(name = "idx_ai_tool_name", columnList = "name"),
        @Index(name = "idx_ai_tool_type", columnList = "tool_type"),
        @Index(name = "idx_ai_tool_enabled", columnList = "enabled")
})
public class AiTool extends AuditableEntity {

    @Id
    private UUID id;

    /**
     * 工具名称（唯一标识，用于 AI 调用）
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 工具显示名称
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 工具描述（用于 AI 理解工具用途）
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 工具类型
     */
    @Column(name = "tool_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ToolType toolType;

    // ==================== 参数 Schema（1对1关联） ====================

    /**
     * 关联的 ExtractionSchema（定义工具参数）
     * 1对1关系：每个工具独占一个 Schema
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "schema_id")
    private ExtractionSchema schema;

    // ==================== API 配置 ====================

    /**
     * API 请求方法
     */
    @Column(name = "api_method", length = 10)
    private String apiMethod;

    /**
     * API URL
     */
    @Column(name = "api_url", length = 500)
    private String apiUrl;

    /**
     * API 请求头（JSON 格式）
     */
    @Column(name = "api_headers", columnDefinition = "TEXT")
    private String apiHeaders;

    /**
     * API 请求体模板（支持变量替换）
     */
    @Column(name = "api_body_template", columnDefinition = "TEXT")
    private String apiBodyTemplate;

    /**
     * API 响应解析路径（JSONPath 格式）
     */
    @Column(name = "api_response_path", length = 200)
    private String apiResponsePath;

    /**
     * API 超时时间（秒）
     */
    @Column(name = "api_timeout")
    private Integer apiTimeout = 30;

    // ==================== MCP 配置 ====================

    /**
     * MCP 服务端点
     */
    @Column(name = "mcp_endpoint", length = 500)
    private String mcpEndpoint;

    /**
     * MCP 工具名称
     */
    @Column(name = "mcp_tool_name", length = 100)
    private String mcpToolName;

    /**
     * MCP 服务器类型（stdio, sse, websocket）
     */
    @Column(name = "mcp_server_type", length = 20)
    private String mcpServerType;

    /**
     * MCP 配置（JSON 格式，包含命令、参数等）
     */
    @Column(name = "mcp_config", columnDefinition = "TEXT")
    private String mcpConfig;

    // ==================== 认证配置 ====================

    /**
     * 认证类型（NONE, API_KEY, BEARER, BASIC, OAUTH2）
     */
    @Column(name = "auth_type", length = 20)
    @Enumerated(EnumType.STRING)
    private AuthType authType = AuthType.NONE;

    /**
     * 认证配置（JSON 格式，根据认证类型包含不同字段）
     */
    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;

    // ==================== 其他配置 ====================

    /**
     * 输入示例（JSON 格式，帮助 AI 理解如何调用）
     */
    @Column(name = "input_example", columnDefinition = "TEXT")
    private String inputExample;

    /**
     * 输出示例（JSON 格式）
     */
    @Column(name = "output_example", columnDefinition = "TEXT")
    private String outputExample;

    /**
     * 返回结果描述（帮助 AI 理解返回数据的含义）
     * 例如："返回用户的订单列表，包含订单号、状态、金额等信息"
     */
    @Column(name = "result_description", columnDefinition = "TEXT")
    private String resultDescription;

    /**
     * 返回字段元数据（JSON 格式，描述返回数据的各个字段含义）
     * 例如：[{"field": "orderId", "type": "string", "description": "订单唯一标识"}]
     */
    @Column(name = "result_metadata", columnDefinition = "TEXT")
    private String resultMetadata;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 是否需要确认（执行前是否需要用户确认）
     */
    @Column(name = "require_confirmation")
    private Boolean requireConfirmation = false;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 标签（JSON 数组，用于分类）
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 工具类型
     */
    public enum ToolType {
        API,        // HTTP API 接口
        MCP,        // MCP 服务
        INTERNAL    // 内部本地工具
    }

    /**
     * 认证类型
     */
    public enum AuthType {
        NONE,       // 无认证
        API_KEY,    // API Key（Header 或 Query）
        BEARER,     // Bearer Token
        BASIC,      // Basic Auth
        OAUTH2      // OAuth2
    }
}

