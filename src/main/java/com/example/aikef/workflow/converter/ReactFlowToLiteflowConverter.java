package com.example.aikef.workflow.converter;

import com.example.aikef.workflow.dto.WorkflowEdgeDto;
import com.example.aikef.workflow.dto.WorkflowNodeDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ReactFlow 到 LiteFlow EL 表达式转换器
 * 
 * 支持将 LLM 节点及其后续流程拆分为独立子链（用于工具调用暂停/恢复）
 */
@Component
public class ReactFlowToLiteflowConverter {

    private static final Logger log = LoggerFactory.getLogger(ReactFlowToLiteflowConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // LLM 节点类型
    private static final String LLM_NODE_TYPE = "llm";

    /**
     * 将 ReactFlow 的 nodes 和 edges JSON 转换为 LiteFlow EL 表达式
     */
    public String convert(String nodesJson, String edgesJson) {
        try {
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            List<WorkflowEdgeDto> edges = objectMapper.readValue(
                    edgesJson, new TypeReference<List<WorkflowEdgeDto>>() {});
            
            return convert(nodes, edges);
        } catch (Exception e) {
            log.error("工作流转换失败", e);
            throw new RuntimeException("工作流转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将节点和边列表转换为 LiteFlow EL 表达式
     */
    public String convert(List<WorkflowNodeDto> nodes, List<WorkflowEdgeDto> edges) {
        // 构建节点映射
        Map<String, WorkflowNodeDto> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeDto::id, n -> n));
        
        // 构建邻接表（出边）
        Map<String, List<EdgeInfo>> outEdges = new HashMap<>();
        // 构建入边映射
        Map<String, List<String>> inEdges = new HashMap<>();
        
        for (WorkflowEdgeDto edge : edges) {
            outEdges.computeIfAbsent(edge.source(), k -> new ArrayList<>())
                    .add(new EdgeInfo(edge.target(), edge.sourceHandle()));
            inEdges.computeIfAbsent(edge.target(), k -> new ArrayList<>())
                    .add(edge.source());
        }
        
        // 找到起始节点（没有入边的节点，或者类型为 start 的节点）
        String startNodeId = findStartNode(nodes, inEdges);
        if (startNodeId == null) {
            throw new IllegalArgumentException("工作流必须有一个起始节点");
        }
        
        // 打印节点和边的调试信息
        log.debug("=== 工作流结构分析 ===");
        log.debug("节点列表:");
        for (WorkflowNodeDto n : nodes) {
            log.debug("  [{}] {} (type: {})", n.id(), 
                     n.data() != null && n.data().label() != null ? n.data().label() : "N/A", n.type());
        }
        log.debug("边列表（节点出边）:");
        for (Map.Entry<String, List<EdgeInfo>> entry : outEdges.entrySet()) {
            WorkflowNodeDto sourceNode = nodeMap.get(entry.getKey());
            String sourceLabel = sourceNode != null && sourceNode.data() != null ? sourceNode.data().label() : "N/A";
            log.debug("  [{}] {} 的出边:", entry.getKey(), sourceLabel);
            for (EdgeInfo edge : entry.getValue()) {
                WorkflowNodeDto targetNode = nodeMap.get(edge.targetId());
                String targetLabel = targetNode != null && targetNode.data() != null ? targetNode.data().label() : "N/A";
                log.debug("    -> [{}] {} (handle: {})", edge.targetId(), targetLabel, edge.sourceHandle());
            }
        }
        log.debug("起始节点: {}", startNodeId);
        
        // 生成 EL 表达式（带格式化）
        Set<String> visited = new HashSet<>();
        String el = generateEl(startNodeId, nodeMap, outEdges, visited, 0);
        
        log.info("生成 LiteFlow EL 表达式:\n{}", el);
        return el;
    }
    
    /**
     * 生成缩进
     */
    private String indent(int level) {
        return "  ".repeat(level);
    }

    /**
     * 找到起始节点
     */
    private String findStartNode(List<WorkflowNodeDto> nodes, Map<String, List<String>> inEdges) {
        // 优先找 start 类型的节点
        for (WorkflowNodeDto node : nodes) {
            if ("start".equals(node.type())) {
                return node.id();
            }
        }
        
        // 找没有入边的节点
        for (WorkflowNodeDto node : nodes) {
            if (!inEdges.containsKey(node.id()) || inEdges.get(node.id()).isEmpty()) {
                return node.id();
            }
        }
        
        // 如果都有入边，返回第一个节点
        return nodes.isEmpty() ? null : nodes.get(0).id();
    }

    /**
     * 递归生成 EL 表达式（带缩进）
     */
    private String generateEl(String nodeId, 
                              Map<String, WorkflowNodeDto> nodeMap,
                              Map<String, List<EdgeInfo>> outEdges,
                              Set<String> visited,
                              int indentLevel) {
        if (visited.contains(nodeId)) {
            // 防止循环
            return "";
        }
        visited.add(nodeId);
        
        WorkflowNodeDto node = nodeMap.get(nodeId);
        if (node == null) {
            return "";
        }
        
        List<EdgeInfo> nextEdges = outEdges.getOrDefault(nodeId, Collections.emptyList());
        String nodeType = node.type();
        
        // 条件节点特殊处理 (IF)
        if ("condition".equals(nodeType)) {
            return generateConditionEl(nodeId, nodeMap, outEdges, nextEdges, visited, indentLevel);
        }
        
        // Switch 节点特殊处理 (SWITCH)
        // 支持 intent、intent_router、tool、parameter_extraction 类型
        if ("intent".equals(nodeType) || "intent_router".equals(nodeType) || 
            "tool".equals(nodeType) || "parameter_extraction".equals(nodeType)) {
            return generateSwitchEl(nodeId, nodeMap, outEdges, nextEdges, visited, indentLevel);
        }
        
        // 普通节点 - 使用 node("nodeType").tag("nodeId") 格式
        StringBuilder el = new StringBuilder();
        el.append(formatNodeRef(nodeType, nodeId));
        
        if (!nextEdges.isEmpty()) {
            if (nextEdges.size() == 1) {
                // 单个后继节点 - 串行
                String nextEl = generateEl(nextEdges.get(0).targetId, nodeMap, outEdges, new HashSet<>(visited), indentLevel);
                if (!nextEl.isEmpty()) {
                    el.append(",\n").append(indent(indentLevel)).append(nextEl);
                }
            } else {
                // 多个后继节点 - 并行
                el.append(",\n").append(indent(indentLevel)).append("WHEN(\n");
                List<String> parallelEls = new ArrayList<>();
                for (EdgeInfo nextEdge : nextEdges) {
                    String nextEl = generateEl(nextEdge.targetId, nodeMap, outEdges, new HashSet<>(visited), indentLevel + 1);
                    if (!nextEl.isEmpty()) {
                        parallelEls.add(indent(indentLevel + 1) + nextEl);
                    }
                }
                el.append(String.join(",\n", parallelEls));
                el.append("\n").append(indent(indentLevel)).append(")");
            }
        }
        
        return el.toString();
    }
    
    /**
     * 格式化节点引用
     * 使用 LiteFlow 的 node 语法: node("componentId").tag("instanceId")
     * 
     * 例如: node("end").tag("2t2dal") 表示使用 end 组件，tag 为 2t2dal
     */
    private String formatNodeRef(String nodeType, String nodeId) {
        // 使用 node().tag() 语法
        return String.format("node(\"%s\").tag(\"%s\")", nodeType, nodeId);
    }

    /**
     * 生成 Switch 节点的 EL 表达式（意图路由）
     * SWITCH(nodeRef).TO(branch1, branch2, ...)
     * 
     * 每个分支是一个完整的子流程，需要递归生成
     */
    private String generateSwitchEl(String nodeId,
                                    Map<String, WorkflowNodeDto> nodeMap,
                                    Map<String, List<EdgeInfo>> outEdges,
                                    List<EdgeInfo> nextEdges,
                                    Set<String> visited,
                                    int indentLevel) {
        WorkflowNodeDto node = nodeMap.get(nodeId);
        String nodeType = node != null ? node.type() : "intent";
        String nodeRef = formatNodeRef(nodeType, nodeId);
        
        if (nextEdges.isEmpty()) {
            return nodeRef;
        }
        
        StringBuilder el = new StringBuilder();
        el.append("SWITCH(").append(nodeRef).append(").TO(\n");
        
        // 为每个分支生成完整的子流程
        List<String> branchEls = new ArrayList<>();
        for (EdgeInfo edge : nextEdges) {
            // 递归生成从目标节点开始的完整流程
            String branchEl = generateEl(edge.targetId, nodeMap, outEdges, new HashSet<>(visited), indentLevel + 1);
            if (!branchEl.isEmpty()) {
                String targetId = edge.targetId();
                // 对于 switch 节点，根据节点类型决定使用哪个作为 tag：
                // - intent 节点返回 "tag:" + targetNodeId，所以使用 targetId 作为 tag
                // - tool 节点也使用 targetId 作为 tag，以便目标节点能获取到正确的配置
                //   注意：tool 节点需要返回 "tag:" + targetNodeId 格式才能匹配
                String branchTag;
                if ("intent".equals(nodeType)) {
                    // intent 节点返回 "tag:" + targetNodeId，所以使用 targetId
                    branchTag = targetId;
                } else {
                    // tool 节点也使用 targetId 作为 tag
                    // 这样目标节点执行时 getTag() 返回的是实际节点ID，而不是状态值
                    branchTag = targetId;
                }
                
                // 根据分支类型添加 tag
                if (branchEl.contains(",") && !branchEl.trim().startsWith("SWITCH(") && 
                    !branchEl.trim().startsWith("IF(") && !branchEl.trim().startsWith("WHEN(") && 
                    !branchEl.trim().startsWith("THEN(")) {
                    // 多节点串行，包装成 THEN 并添加 tag
                    branchEl = "THEN(\n" + indent(indentLevel + 2) + branchEl.replace("\n", "\n" + indent(1)) + 
                               "\n" + indent(indentLevel + 1) + ").tag(\"" + branchTag + "\")";
                } else if (branchEl.trim().startsWith("SWITCH(") || branchEl.trim().startsWith("IF(")) {
                    // 嵌套的 SWITCH 或 IF，在末尾添加 tag
                    branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                } else {
                    // 单节点分支：需要替换或添加 tag
                    // 如果已经有 tag，替换它；如果没有，添加它
                    if (branchEl.contains(".tag(")) {
                        // 替换现有的 tag
                        branchEl = branchEl.replaceAll("\\.tag\\(\"[^\"]+\"\\)", ".tag(\"" + branchTag + "\")");
                    } else {
                        // 添加新的 tag
                        branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                    }
                }
                
                branchEls.add(indent(indentLevel + 1) + branchEl);
            }
        }
        
        if (branchEls.isEmpty()) {
            return nodeRef;
        }
        
        // 去重
        branchEls = branchEls.stream().distinct().toList();
        
        el.append(String.join(",\n", branchEls));
        el.append("\n").append(indent(indentLevel)).append(")");
        
        return el.toString();
    }

    /**
     * 生成条件节点的 EL 表达式
     */
    private String generateConditionEl(String nodeId,
                                        Map<String, WorkflowNodeDto> nodeMap,
                                        Map<String, List<EdgeInfo>> outEdges,
                                        List<EdgeInfo> nextEdges,
                                        Set<String> visited,
                                        int indentLevel) {
        WorkflowNodeDto node = nodeMap.get(nodeId);
        String nodeType = node != null ? node.type() : "condition";
        String nodeRef = formatNodeRef(nodeType, nodeId);
        
        // IF(condition, trueBranch, falseBranch)
        String trueBranch = "";
        String falseBranch = "";
        
        for (EdgeInfo edge : nextEdges) {
            String branchEl = generateEl(edge.targetId, nodeMap, outEdges, new HashSet<>(visited), indentLevel + 1);
            if ("true".equals(edge.sourceHandle) || "yes".equals(edge.sourceHandle)) {
                trueBranch = branchEl;
            } else if ("false".equals(edge.sourceHandle) || "no".equals(edge.sourceHandle)) {
                falseBranch = branchEl;
            } else if (trueBranch.isEmpty()) {
                trueBranch = branchEl;
            } else {
                falseBranch = branchEl;
            }
        }
        
        StringBuilder el = new StringBuilder();
        el.append("IF(\n");
        el.append(indent(indentLevel + 1)).append(nodeRef).append(",\n");
        el.append(indent(indentLevel + 1)).append("THEN(\n");
        el.append(indent(indentLevel + 2)).append(trueBranch.replace("\n", "\n" + indent(1))).append("\n");
        el.append(indent(indentLevel + 1)).append(")");
        
        if (!falseBranch.isEmpty()) {
            el.append(",\n");
            el.append(indent(indentLevel + 1)).append("THEN(\n");
            el.append(indent(indentLevel + 2)).append(falseBranch.replace("\n", "\n" + indent(1))).append("\n");
            el.append(indent(indentLevel + 1)).append(")");
        }
        
        el.append("\n").append(indent(indentLevel)).append(")");
        
        return el.toString();
    }

    /**
     * 边信息
     */
    private record EdgeInfo(String targetId, String sourceHandle) {}

    /**
     * 验证工作流结构
     */
    public ValidationResult validate(String nodesJson, String edgesJson) {
        try {
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            List<WorkflowEdgeDto> edges = objectMapper.readValue(
                    edgesJson, new TypeReference<List<WorkflowEdgeDto>>() {});
            
            return validate(nodes, edges);
        } catch (Exception e) {
            return new ValidationResult(false, List.of("JSON 解析失败: " + e.getMessage()));
        }
    }

    public ValidationResult validate(List<WorkflowNodeDto> nodes, List<WorkflowEdgeDto> edges) {
        List<String> errors = new ArrayList<>();
        
        // 检查是否有节点
        if (nodes == null || nodes.isEmpty()) {
            errors.add("工作流必须至少包含一个节点");
            return new ValidationResult(false, errors);
        }
        
        Set<String> nodeIds = nodes.stream().map(WorkflowNodeDto::id).collect(Collectors.toSet());
        
        // 检查边的引用是否有效
        if (edges != null) {
            for (WorkflowEdgeDto edge : edges) {
                if (!nodeIds.contains(edge.source())) {
                    errors.add("边的源节点不存在: " + edge.source());
                }
                if (!nodeIds.contains(edge.target())) {
                    errors.add("边的目标节点不存在: " + edge.target());
                }
            }
        }
        
        // 检查是否有起始节点
        Map<String, List<String>> inEdges = new HashMap<>();
        if (edges != null) {
            for (WorkflowEdgeDto edge : edges) {
                inEdges.computeIfAbsent(edge.target(), k -> new ArrayList<>()).add(edge.source());
            }
        }
        
        boolean hasStart = nodes.stream().anyMatch(n -> 
                "start".equals(n.type()) || !inEdges.containsKey(n.id()));
        if (!hasStart) {
            errors.add("工作流必须有一个起始节点（没有入边的节点或 start 类型节点）");
        }
        
        // 检查条件节点的分支
        for (WorkflowNodeDto node : nodes) {
            if ("condition".equals(node.type())) {
                long branchCount = edges != null ? 
                        edges.stream().filter(e -> e.source().equals(node.id())).count() : 0;
                if (branchCount < 1) {
                    errors.add("条件节点 " + node.id() + " 至少需要一个分支");
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证结果
     */
    public record ValidationResult(boolean valid, List<String> errors) {}

    // ==================== LLM 子链拆分功能 ====================

    /**
     * 转换结果（包含主链和子链）
     */
    public record ConversionResult(
            String mainChainEl,                    // 主链 EL 表达式
            Map<String, SubChainInfo> subChains,   // 子链映射 (llmNodeId -> SubChainInfo)
            List<String> llmNodeIds                // 所有 LLM 节点 ID
    ) {}

    /**
     * 子链信息
     */
    public record SubChainInfo(
            String chainId,       // 子链 ID
            String chainEl,       // 子链 EL 表达式
            String llmNodeId,     // LLM 节点 ID
            List<String> nodeIds  // 子链包含的所有节点 ID
    ) {}

    /**
     * 带子链拆分的转换
     * 将 LLM 节点及其后续节点拆分为独立子链
     */
    public ConversionResult convertWithSubChains(String nodesJson, String edgesJson, String workflowId) {
        try {
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            List<WorkflowEdgeDto> edges = objectMapper.readValue(
                    edgesJson, new TypeReference<List<WorkflowEdgeDto>>() {});
            
            return convertWithSubChains(nodes, edges, workflowId);
        } catch (Exception e) {
            log.error("工作流转换失败", e);
            throw new RuntimeException("工作流转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带子链拆分的转换
     */
    public ConversionResult convertWithSubChains(List<WorkflowNodeDto> nodes, 
                                                  List<WorkflowEdgeDto> edges, 
                                                  String workflowId) {
        // 构建节点映射
        Map<String, WorkflowNodeDto> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeDto::id, n -> n));
        
        // 构建邻接表（出边）
        Map<String, List<EdgeInfo>> outEdges = new HashMap<>();
        // 构建入边映射
        Map<String, List<String>> inEdges = new HashMap<>();
        
        for (WorkflowEdgeDto edge : edges) {
            outEdges.computeIfAbsent(edge.source(), k -> new ArrayList<>())
                    .add(new EdgeInfo(edge.target(), edge.sourceHandle()));
            inEdges.computeIfAbsent(edge.target(), k -> new ArrayList<>())
                    .add(edge.source());
        }
        
        // 找到所有 LLM 节点
        List<String> llmNodeIds = nodes.stream()
                .filter(n -> LLM_NODE_TYPE.equals(n.type()))
                .map(WorkflowNodeDto::id)
                .toList();
        
        Set<String> allLlmNodeIds = new HashSet<>(llmNodeIds);
        
        log.info("发现 {} 个 LLM 节点: {}", llmNodeIds.size(), llmNodeIds);
        
        // 为每个 LLM 节点生成子链
        Map<String, SubChainInfo> subChains = new HashMap<>();
        
        for (String llmNodeId : llmNodeIds) {
            SubChainInfo subChain = generateSubChain(llmNodeId, nodeMap, outEdges, allLlmNodeIds, workflowId);
            if (subChain != null) {
                subChains.put(llmNodeId, subChain);
                log.info("生成子链: chainId={}, llmNodeId={}, nodes={}", 
                        subChain.chainId(), llmNodeId, subChain.nodeIds());
            }
        }
        
        // 生成主链 EL（主链中 LLM 节点后的部分替换为子链调用）
        String mainChainEl = generateMainChainEl(nodes, edges, nodeMap, outEdges, inEdges, subChains);
        
        return new ConversionResult(mainChainEl, subChains, llmNodeIds);
    }

    /**
     * 为 LLM 节点生成子链
     * 子链包含 LLM 节点及其后续节点，直到遇到下一个 LLM 节点为止
     */
    private SubChainInfo generateSubChain(String llmNodeId,
                                           Map<String, WorkflowNodeDto> nodeMap,
                                           Map<String, List<EdgeInfo>> outEdges,
                                           Set<String> allLlmNodeIds,
                                           String workflowId) {
        WorkflowNodeDto llmNode = nodeMap.get(llmNodeId);
        if (llmNode == null) {
            return null;
        }
        
        // 收集 LLM 节点及其后续节点（遇到另一个 LLM 时停止）
        Set<String> subChainNodes = new LinkedHashSet<>();
        collectDownstreamNodesUntilLlm(llmNodeId, nodeMap, outEdges, allLlmNodeIds, subChainNodes);
        
        if (subChainNodes.isEmpty()) {
            return null;
        }
        
        // 生成子链 ID
        String chainId = String.format("subchain_%s_%s", workflowId, llmNodeId);
        
        // 生成子链 EL 表达式（遇到下一个 LLM 时用 CHAIN 调用）
        Set<String> visited = new HashSet<>();
        String chainEl = generateSubChainEl(llmNodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, visited, 0);
        
        // 如果子链 EL 为空，跳过此子链
        if (chainEl == null || chainEl.isBlank()) {
            log.warn("子链 EL 为空，跳过: llmNodeId={}", llmNodeId);
            return null;
        }
        
        // 包装成完整的链表达式
        if (!chainEl.contains(",")) {
            chainEl = "THEN(" + chainEl + ")";
        } else {
            chainEl = "THEN(\n  " + chainEl.replace("\n", "\n  ") + "\n)";
        }
        
        return new SubChainInfo(
                chainId,
                chainEl,
                llmNodeId,
                new ArrayList<>(subChainNodes)
        );
    }

    /**
     * 递归收集下游节点，遇到另一个 LLM 节点时停止（不包含该 LLM）
     */
    private void collectDownstreamNodesUntilLlm(String nodeId,
                                                 Map<String, WorkflowNodeDto> nodeMap,
                                                 Map<String, List<EdgeInfo>> outEdges,
                                                 Set<String> allLlmNodeIds,
                                                 Set<String> collected) {
        if (collected.contains(nodeId)) {
            return;
        }
        collected.add(nodeId);
        
        List<EdgeInfo> nextEdges = outEdges.getOrDefault(nodeId, Collections.emptyList());
        for (EdgeInfo edge : nextEdges) {
            String nextNodeId = edge.targetId();
            // 如果下一个节点是另一个 LLM，停止收集（但不包含该 LLM）
            if (allLlmNodeIds.contains(nextNodeId) && !nextNodeId.equals(nodeId)) {
                continue;
            }
            collectDownstreamNodesUntilLlm(nextNodeId, nodeMap, outEdges, allLlmNodeIds, collected);
        }
    }

    /**
     * 生成子链 EL 表达式，遇到下一个 LLM 时用 CHAIN 调用
     */
    private String generateSubChainEl(String nodeId,
                                       Map<String, WorkflowNodeDto> nodeMap,
                                       Map<String, List<EdgeInfo>> outEdges,
                                       Set<String> allLlmNodeIds,
                                       String workflowId,
                                       Set<String> visited,
                                       int indentLevel) {
        if (visited.contains(nodeId)) {
            return "";
        }
        visited.add(nodeId);
        
        WorkflowNodeDto node = nodeMap.get(nodeId);
        if (node == null) {
            return "";
        }
        
        String nodeType = node.type();
        List<EdgeInfo> nextEdges = outEdges.getOrDefault(nodeId, Collections.emptyList());
        
        // 条件节点
        if ("condition".equals(nodeType)) {
            return generateConditionElForSubChain(nodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, nextEdges, visited, indentLevel);
        }
        
        // 意图节点
        if ("intent".equals(nodeType) || "intent_router".equals(nodeType) || 
            "tool".equals(nodeType)) {
            return generateSwitchElForSubChain(nodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, nextEdges, visited, indentLevel);
        }
        
        // 普通节点（包括当前子链的 LLM 节点）
        StringBuilder el = new StringBuilder();
        el.append(formatNodeRef(nodeType, nodeId));
        
        if (!nextEdges.isEmpty()) {
            if (nextEdges.size() == 1) {
                String nextNodeId = nextEdges.get(0).targetId();
                // 如果下一个节点是另一个 LLM，用子链调用（子链 ID 直接作为节点使用，不需要 .tag()）
                if (allLlmNodeIds.contains(nextNodeId)) {
                    String nextChainId = String.format("subchain_%s_%s", workflowId, nextNodeId);
                    // 直接使用子链 ID，LiteFlow 会将其解析为对子链的调用
                    el.append(",\n").append(indent(indentLevel)).append(nextChainId);
                } else {
                    String nextEl = generateSubChainEl(nextNodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, new HashSet<>(visited), indentLevel);
                    if (!nextEl.isEmpty()) {
                        el.append(",\n").append(indent(indentLevel)).append(nextEl);
                    }
                }
            } else {
                // 多个后继节点
                el.append(",\n").append(indent(indentLevel)).append("WHEN(\n");
                List<String> parallelEls = new ArrayList<>();
                for (EdgeInfo nextEdge : nextEdges) {
                    String nextNodeId = nextEdge.targetId();
                    String nextEl;
                    if (allLlmNodeIds.contains(nextNodeId)) {
                        String nextChainId = String.format("subchain_%s_%s", workflowId, nextNodeId);
                        // 直接使用子链 ID
                        nextEl = nextChainId;
                    } else {
                        nextEl = generateSubChainEl(nextNodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, new HashSet<>(visited), indentLevel + 1);
                    }
                    if (!nextEl.isEmpty()) {
                        parallelEls.add(indent(indentLevel + 1) + nextEl);
                    }
                }
                if (!parallelEls.isEmpty()) {
                    el.append(String.join(",\n", parallelEls));
                    el.append("\n").append(indent(indentLevel)).append(")");
                }
            }
        }
        
        return el.toString();
    }

    /**
     * 生成子链中条件节点的 EL
     */
    private String generateConditionElForSubChain(String nodeId,
                                                   Map<String, WorkflowNodeDto> nodeMap,
                                                   Map<String, List<EdgeInfo>> outEdges,
                                                   Set<String> allLlmNodeIds,
                                                   String workflowId,
                                                   List<EdgeInfo> nextEdges,
                                                   Set<String> visited,
                                                   int indentLevel) {
        WorkflowNodeDto node = nodeMap.get(nodeId);
        String nodeType = node != null ? node.type() : "condition";
        String nodeRef = formatNodeRef(nodeType, nodeId);
        
        String trueBranch = "";
        String falseBranch = "";
        
        for (EdgeInfo edge : nextEdges) {
            String nextNodeId = edge.targetId();
            String branchEl;
            if (allLlmNodeIds.contains(nextNodeId)) {
                // 直接使用子链 ID，不需要 .tag()
                branchEl = String.format("subchain_%s_%s", workflowId, nextNodeId);
            } else {
                branchEl = generateSubChainEl(nextNodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, new HashSet<>(visited), indentLevel + 1);
            }
            
            if ("true".equals(edge.sourceHandle()) || "yes".equals(edge.sourceHandle())) {
                trueBranch = branchEl;
            } else if ("false".equals(edge.sourceHandle()) || "no".equals(edge.sourceHandle())) {
                falseBranch = branchEl;
            } else if (trueBranch.isEmpty()) {
                trueBranch = branchEl;
            } else {
                falseBranch = branchEl;
            }
        }
        
        StringBuilder el = new StringBuilder();
        el.append("IF(\n");
        el.append(indent(indentLevel + 1)).append(nodeRef).append(",\n");
        el.append(indent(indentLevel + 1)).append("THEN(\n");
        el.append(indent(indentLevel + 2)).append(trueBranch.replace("\n", "\n" + indent(1))).append("\n");
        el.append(indent(indentLevel + 1)).append(")");
        
        if (!falseBranch.isEmpty()) {
            el.append(",\n");
            el.append(indent(indentLevel + 1)).append("THEN(\n");
            el.append(indent(indentLevel + 2)).append(falseBranch.replace("\n", "\n" + indent(1))).append("\n");
            el.append(indent(indentLevel + 1)).append(")");
        }
        
        el.append("\n").append(indent(indentLevel)).append(")");
        
        return el.toString();
    }

    /**
     * 生成子链中 Switch 节点的 EL
     */
    private String generateSwitchElForSubChain(String nodeId,
                                                Map<String, WorkflowNodeDto> nodeMap,
                                                Map<String, List<EdgeInfo>> outEdges,
                                                Set<String> allLlmNodeIds,
                                                String workflowId,
                                                List<EdgeInfo> nextEdges,
                                                Set<String> visited,
                                                int indentLevel) {
        WorkflowNodeDto node = nodeMap.get(nodeId);
        String nodeType = node != null ? node.type() : "intent";
        String nodeRef = formatNodeRef(nodeType, nodeId);
        
        if (nextEdges.isEmpty()) {
            return nodeRef;
        }
        
        StringBuilder el = new StringBuilder();
        el.append("SWITCH(").append(nodeRef).append(").TO(\n");
        
        List<String> branchEls = new ArrayList<>();
        for (EdgeInfo edge : nextEdges) {
            String nextNodeId = edge.targetId();
            String branchEl;
            if (allLlmNodeIds.contains(nextNodeId)) {
                // 直接使用子链 ID，不需要 .tag()
                branchEl = String.format("subchain_%s_%s", workflowId, nextNodeId);
            } else {
                branchEl = generateSubChainEl(nextNodeId, nodeMap, outEdges, allLlmNodeIds, workflowId, new HashSet<>(visited), indentLevel + 1);
            }
            
            if (!branchEl.isEmpty()) {
                String targetId = edge.targetId();
                // 对于 switch 节点，根据节点类型决定使用哪个作为 tag：
                // - intent 节点返回 "tag:" + targetNodeId，所以使用 targetId 作为 tag
                // - tool 节点也使用 targetId 作为 tag，以便目标节点能获取到正确的配置
                String branchTag;
                if ("intent".equals(nodeType)) {
                    branchTag = targetId;
                } else {
                    // tool 节点也使用 targetId 作为 tag
                    // 这样目标节点执行时 getTag() 返回的是实际节点ID，而不是状态值
                    branchTag = targetId;
                }
                
                // 子链不需要额外包装
                if (branchEl.trim().startsWith("subchain_")) {
                    // 子链直接使用，添加 tag 用于路由
                    branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                } else if (branchEl.contains(",") && !branchEl.trim().startsWith("SWITCH(") && 
                    !branchEl.trim().startsWith("IF(") && !branchEl.trim().startsWith("WHEN(") && 
                    !branchEl.trim().startsWith("THEN(")) {
                    branchEl = "THEN(\n" + indent(indentLevel + 2) + branchEl.replace("\n", "\n" + indent(1)) + 
                               "\n" + indent(indentLevel + 1) + ").tag(\"" + branchTag + "\")";
                } else if (branchEl.trim().startsWith("SWITCH(") || branchEl.trim().startsWith("IF(")) {
                    branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                } else {
                    // 单节点分支：需要替换或添加 tag
                    if (branchEl.contains(".tag(")) {
                        branchEl = branchEl.replaceAll("\\.tag\\(\"[^\"]+\"\\)", ".tag(\"" + branchTag + "\")");
                    } else {
                        branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                    }
                }
                
                branchEls.add(indent(indentLevel + 1) + branchEl);
            }
        }
        
        if (branchEls.isEmpty()) {
            return nodeRef;
        }
        
        branchEls = branchEls.stream().distinct().toList();
        
        el.append(String.join(",\n", branchEls));
        el.append("\n").append(indent(indentLevel)).append(")");
        
        return el.toString();
    }

    /**
     * 生成主链 EL（LLM 节点替换为子链调用点）
     */
    private String generateMainChainEl(List<WorkflowNodeDto> nodes,
                                        List<WorkflowEdgeDto> edges,
                                        Map<String, WorkflowNodeDto> nodeMap,
                                        Map<String, List<EdgeInfo>> outEdges,
                                        Map<String, List<String>> inEdges,
                                        Map<String, SubChainInfo> subChains) {
        // 找到起始节点
        String startNodeId = findStartNode(nodes, inEdges);
        if (startNodeId == null) {
            throw new IllegalArgumentException("工作流必须有一个起始节点");
        }
        
        // 生成主链 EL，遇到 LLM 节点时使用 CHAIN 调用
        Set<String> visited = new HashSet<>();
        String el = generateMainChainElRecursive(startNodeId, nodeMap, outEdges, subChains, visited, 0);
        
        log.info("生成主链 EL:\n{}", el);
        return el;
    }

    /**
     * 递归生成主链 EL
     * LLM 节点会被替换为 CHAIN 调用，LLM 后续节点不在主链中生成（由子链包含）
     */
    private String generateMainChainElRecursive(String nodeId,
                                                 Map<String, WorkflowNodeDto> nodeMap,
                                                 Map<String, List<EdgeInfo>> outEdges,
                                                 Map<String, SubChainInfo> subChains,
                                                 Set<String> visited,
                                                 int indentLevel) {
        if (visited.contains(nodeId)) {
            return "";
        }
        visited.add(nodeId);
        
        WorkflowNodeDto node = nodeMap.get(nodeId);
        if (node == null) {
            return "";
        }
        
        String nodeType = node.type();
        List<EdgeInfo> nextEdges = outEdges.getOrDefault(nodeId, Collections.emptyList());
        
        // 检查是否是 LLM 节点 - 替换为子链调用，不再生成后续节点
        if (LLM_NODE_TYPE.equals(nodeType) && subChains.containsKey(nodeId)) {
            SubChainInfo subChain = subChains.get(nodeId);
            // 直接使用子链 ID 作为节点引用（LiteFlow 会自动识别并执行子链）
            return String.format("%s.tag(\"%s\")", subChain.chainId(), nodeId);
        }
        
        // 条件节点特殊处理
        if ("condition".equals(nodeType)) {
            return generateConditionElForMainChain(nodeId, nodeMap, outEdges, subChains, nextEdges, visited, indentLevel);
        }
        
        // 意图节点特殊处理
        if ("intent".equals(nodeType) || "intent_router".equals(nodeType) || 
            "tool".equals(nodeType)) {
            return generateSwitchElForMainChain(nodeId, nodeMap, outEdges, subChains, nextEdges, visited, indentLevel);
        }
        
        // 普通节点
        StringBuilder el = new StringBuilder();
        el.append(formatNodeRef(nodeType, nodeId));
        
        if (!nextEdges.isEmpty()) {
            if (nextEdges.size() == 1) {
                String nextNodeId = nextEdges.get(0).targetId();
                String nextEl = generateMainChainElRecursive(nextNodeId, nodeMap, outEdges, subChains, new HashSet<>(visited), indentLevel);
                if (!nextEl.isEmpty()) {
                    el.append(",\n").append(indent(indentLevel)).append(nextEl);
                }
            } else {
                // 多个后继节点 - 并行
                el.append(",\n").append(indent(indentLevel)).append("WHEN(\n");
                List<String> parallelEls = new ArrayList<>();
                for (EdgeInfo nextEdge : nextEdges) {
                    String nextEl = generateMainChainElRecursive(nextEdge.targetId(), nodeMap, outEdges, subChains, new HashSet<>(visited), indentLevel + 1);
                    if (!nextEl.isEmpty()) {
                        parallelEls.add(indent(indentLevel + 1) + nextEl);
                    }
                }
                if (!parallelEls.isEmpty()) {
                    el.append(String.join(",\n", parallelEls));
                    el.append("\n").append(indent(indentLevel)).append(")");
                }
            }
        }
        
        return el.toString();
    }

    /**
     * 生成主链中的条件节点 EL
     */
    private String generateConditionElForMainChain(String nodeId,
                                                    Map<String, WorkflowNodeDto> nodeMap,
                                                    Map<String, List<EdgeInfo>> outEdges,
                                                    Map<String, SubChainInfo> subChains,
                                                    List<EdgeInfo> nextEdges,
                                                    Set<String> visited,
                                                    int indentLevel) {
        WorkflowNodeDto node = nodeMap.get(nodeId);
        String nodeType = node != null ? node.type() : "condition";
        String nodeRef = formatNodeRef(nodeType, nodeId);
        
        String trueBranch = "";
        String falseBranch = "";
        
        for (EdgeInfo edge : nextEdges) {
            String branchEl = generateMainChainElRecursive(edge.targetId(), nodeMap, outEdges, subChains, new HashSet<>(visited), indentLevel + 1);
            if ("true".equals(edge.sourceHandle) || "yes".equals(edge.sourceHandle)) {
                trueBranch = branchEl;
            } else if ("false".equals(edge.sourceHandle) || "no".equals(edge.sourceHandle)) {
                falseBranch = branchEl;
            } else if (trueBranch.isEmpty()) {
                trueBranch = branchEl;
            } else {
                falseBranch = branchEl;
            }
        }
        
        StringBuilder el = new StringBuilder();
        el.append("IF(\n");
        el.append(indent(indentLevel + 1)).append(nodeRef).append(",\n");
        el.append(indent(indentLevel + 1)).append("THEN(\n");
        el.append(indent(indentLevel + 2)).append(trueBranch.replace("\n", "\n" + indent(1))).append("\n");
        el.append(indent(indentLevel + 1)).append(")");
        
        if (!falseBranch.isEmpty()) {
            el.append(",\n");
            el.append(indent(indentLevel + 1)).append("THEN(\n");
            el.append(indent(indentLevel + 2)).append(falseBranch.replace("\n", "\n" + indent(1))).append("\n");
            el.append(indent(indentLevel + 1)).append(")");
        }
        
        el.append("\n").append(indent(indentLevel)).append(")");
        
        return el.toString();
    }

    /**
     * 生成主链中的 Switch 节点 EL
     */
    private String generateSwitchElForMainChain(String nodeId,
                                                 Map<String, WorkflowNodeDto> nodeMap,
                                                 Map<String, List<EdgeInfo>> outEdges,
                                                 Map<String, SubChainInfo> subChains,
                                                 List<EdgeInfo> nextEdges,
                                                 Set<String> visited,
                                                 int indentLevel) {
        WorkflowNodeDto node = nodeMap.get(nodeId);
        String nodeType = node != null ? node.type() : "intent";
        String nodeRef = formatNodeRef(nodeType, nodeId);
        
        if (nextEdges.isEmpty()) {
            return nodeRef;
        }
        
        StringBuilder el = new StringBuilder();
        el.append("SWITCH(").append(nodeRef).append(").TO(\n");
        
        List<String> branchEls = new ArrayList<>();
        for (EdgeInfo edge : nextEdges) {
            String branchEl = generateMainChainElRecursive(edge.targetId(), nodeMap, outEdges, subChains, new HashSet<>(visited), indentLevel + 1);
            if (!branchEl.isEmpty()) {
                String targetId = edge.targetId();
                // 对于 switch 节点，根据节点类型决定使用哪个作为 tag：
                // - intent 节点返回 "tag:" + targetNodeId，所以使用 targetId 作为 tag
                // - tool 节点也使用 targetId 作为 tag，以便目标节点能获取到正确的配置
                String branchTag;
                if ("intent".equals(nodeType)) {
                    branchTag = targetId;
                } else {
                    // tool 节点也使用 targetId 作为 tag
                    // 这样目标节点执行时 getTag() 返回的是实际节点ID，而不是状态值
                    branchTag = targetId;
                }
                
                if (branchEl.contains(",") && !branchEl.trim().startsWith("SWITCH(") && 
                    !branchEl.trim().startsWith("IF(") && !branchEl.trim().startsWith("WHEN(") && 
                    !branchEl.trim().startsWith("THEN(") && !branchEl.trim().startsWith("subchain_")) {
                    branchEl = "THEN(\n" + indent(indentLevel + 2) + branchEl.replace("\n", "\n" + indent(1)) + 
                               "\n" + indent(indentLevel + 1) + ").tag(\"" + branchTag + "\")";
                } else if (branchEl.trim().startsWith("SWITCH(") || branchEl.trim().startsWith("IF(")) {
                    branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                } else {
                    // 单节点分支：需要替换或添加 tag
                    if (branchEl.contains(".tag(")) {
                        branchEl = branchEl.replaceAll("\\.tag\\(\"[^\"]+\"\\)", ".tag(\"" + branchTag + "\")");
                    } else {
                        branchEl = branchEl + ".tag(\"" + branchTag + "\")";
                    }
                }
                
                branchEls.add(indent(indentLevel + 1) + branchEl);
            }
        }
        
        if (branchEls.isEmpty()) {
            return nodeRef;
        }
        
        branchEls = branchEls.stream().distinct().toList();
        
        el.append(String.join(",\n", branchEls));
        el.append("\n").append(indent(indentLevel)).append(")");
        
        return el.toString();
    }
}


