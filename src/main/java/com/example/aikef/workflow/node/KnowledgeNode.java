package com.example.aikef.workflow.node;

import com.example.aikef.knowledge.KnowledgeBaseService;
import com.example.aikef.knowledge.VectorStoreService;
import com.example.aikef.model.KnowledgeBase;
import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库查询节点
 * 使用 Redis 向量存储进行语义搜索
 */
@LiteflowComponent("knowledge")
public class KnowledgeNode extends BaseWorkflowNode {

    @Resource
    private VectorStoreService vectorStoreService;
    
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 获取配置
            String querySource = getConfigString("querySource", "query");
            String query = switch (querySource) {
                case "query", "userMessage" -> ctx.getQuery();
                case "lastOutput", "previousOutput" -> ctx.getLastOutput();
                case "custom" -> getConfigString("customQuery", ctx.getQuery());
                default -> ctx.getQuery();
            };
            
            Integer maxResults = getConfigInt("maxResults", 3);
            Double minScore = getConfigDouble("minScore", 0.5);
            String outputFormat = getConfigString("outputFormat", "combined"); // combined, list, first, json
            
            // 获取知识库 ID（支持单个或多个）
            List<UUID> knowledgeBaseIds = getKnowledgeBaseIds();
            
            if (knowledgeBaseIds.isEmpty()) {
                // 如果未指定，使用所有启用的知识库
                knowledgeBaseIds = knowledgeBaseService.getEnabledKnowledgeBases()
                        .stream()
                        .map(KnowledgeBase::getId)
                        .toList();
            }
            
            if (knowledgeBaseIds.isEmpty()) {
                String output = getConfigString("noResultMessage", "未配置知识库。");
                setOutput(output);
                recordExecution(query, output, startTime, true, null);
                return;
            }
            
            // 执行向量搜索
            List<VectorStoreService.SearchResult> results;
            if (knowledgeBaseIds.size() == 1) {
                results = vectorStoreService.search(knowledgeBaseIds.get(0), query, maxResults, minScore);
            } else {
                results = vectorStoreService.searchMultiple(knowledgeBaseIds, query, maxResults, minScore);
            }
            
            String output;
            if (results.isEmpty()) {
                output = getConfigString("noResultMessage", "未找到相关知识。");
            } else {
                output = formatOutput(results, outputFormat);
            }
            
            // 保存到上下文变量
            ctx.setVariable("knowledgeResults", results);
            ctx.setVariable("knowledgeContent", output);
            ctx.setVariable("knowledgeResultCount", results.size());
            
            // 保存详细结果供后续节点使用
            List<String> contents = results.stream()
                    .map(VectorStoreService.SearchResult::getContent)
                    .toList();
            ctx.setVariable("knowledgeContents", contents);
            
            log.info("知识库向量搜索完成: query={}, resultCount={}, knowledgeBases={}", 
                    query.substring(0, Math.min(50, query.length())), 
                    results.size(),
                    knowledgeBaseIds.size());
            
            setOutput(output);
            recordExecution(query, output, startTime, true, null);
            
        } catch (Exception e) {
            log.error("知识库查询失败", e);
            String errorOutput = getConfigString("errorMessage", "知识库查询失败");
            setOutput(errorOutput);
            recordExecution(null, errorOutput, startTime, false, e.getMessage());
        }
    }

    /**
     * 获取配置的知识库 ID 列表
     */
    @SuppressWarnings("unchecked")
    private List<UUID> getKnowledgeBaseIds() {
        List<UUID> ids = new ArrayList<>();
        
        // 单个知识库 ID
        String singleId = getConfigString("knowledgeBaseId", null);
        if (singleId != null && !singleId.isEmpty()) {
            try {
                ids.add(UUID.fromString(singleId));
            } catch (Exception e) {
                log.warn("无效的知识库 ID: {}", singleId);
            }
        }
        
        // 多个知识库 ID
        Object multiIds = getNodeConfig().get("knowledgeBaseIds");
        if (multiIds instanceof List<?> idList) {
            for (Object item : idList) {
                try {
                    if (item instanceof String s) {
                        ids.add(UUID.fromString(s));
                    } else if (item instanceof UUID u) {
                        ids.add(u);
                    }
                } catch (Exception e) {
                    log.warn("无效的知识库 ID: {}", item);
                }
            }
        }
        
        return ids;
    }

    /**
     * 格式化输出
     */
    private String formatOutput(List<VectorStoreService.SearchResult> results, String format) {
        return switch (format) {
            case "first" -> results.get(0).getContent();
            case "list" -> results.stream()
                    .map(r -> {
                        String title = r.getTitle() != null ? r.getTitle() : "无标题";
                        return String.format("- **%s** (相似度: %.2f)\n  %s", 
                                title, r.getScore(), r.getContent());
                    })
                    .collect(Collectors.joining("\n\n"));
            case "json" -> toJson(results);
            case "combined" -> results.stream()
                    .map(VectorStoreService.SearchResult::getContent)
                    .collect(Collectors.joining("\n\n---\n\n"));
            default -> results.get(0).getContent();
        };
    }

    private String toJson(List<VectorStoreService.SearchResult> results) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            VectorStoreService.SearchResult r = results.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"title\":\"%s\",\"content\":\"%s\",\"score\":%.4f}",
                    escapeJson(r.getTitle()),
                    escapeJson(r.getContent()),
                    r.getScore()
            ));
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 获取 Double 类型配置
     */
    protected Double getConfigDouble(String key, Double defaultValue) {
        Object value = getNodeConfig().get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
