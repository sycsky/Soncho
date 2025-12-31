package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * AI 工作流实体
 * 存储来自 ReactFlow 前端编辑器的工作流定义
 */
@Entity
@Table(name = "ai_workflows")
@Data
public class AiWorkflow extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;
    @Column(nullable = false)
    private String name;

    /**
     * 工作流描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * ReactFlow 节点数据 (JSON)
     */
    @Column(name = "nodes_json", columnDefinition = "LONGTEXT")
    private String nodesJson;

    /**
     * ReactFlow 边数据 (JSON)
     */
    @Column(name = "edges_json", columnDefinition = "LONGTEXT")
    private String edgesJson;

    /**
     * LiteFlow EL 表达式 (由 nodes/edges 转换生成)
     */
    @Column(name = "liteflow_el", columnDefinition = "TEXT")
    private String liteflowEl;

    /**
     * 子链信息 (JSON)
     * 存储 LLM 节点拆分的子链信息，用于工具调用暂停/恢复
     * 格式: { "llmNodeId": { "chainId": "xxx", "chainEl": "xxx", "nodeIds": [...] } }
     */
    @Column(name = "sub_chains_json", columnDefinition = "TEXT")
    private String subChainsJson;

    /**
     * LLM 节点ID列表 (JSON)
     */
    @Column(name = "llm_node_ids", columnDefinition = "TEXT")
    private String llmNodeIds;

    /**
     * 工作流版本号
     */
    @Column(nullable = false)
    private Integer version = 1;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = false;

    /**
     * 是否为默认工作流
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    /**
     * 创建者
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_agent_id")
    private Agent createdByAgent;

    /**
     * 触发条件类型
     * ALL - 所有会话
     * CATEGORY - 指定分类
     * KEYWORD - 关键词匹配
     */
    @Column(name = "trigger_type")
    private String triggerType = "ALL";

    /**
     * 触发条件配置 (JSON)
     * 如分类ID列表、关键词列表等
     */
    @Column(name = "trigger_config", columnDefinition = "TEXT")
    private String triggerConfig;

    // Getters and Setters


}


