package com.example.aikef.workflow.service;

import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.model.WorkflowPausedState;
import com.example.aikef.workflow.repository.WorkflowPausedStateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 工作流暂停服务
 * 管理因工具调用等待用户输入而暂停的工作流状态
 */
@Service
@Transactional
public class WorkflowPauseService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowPauseService.class);

    @Resource
    private WorkflowPausedStateRepository pausedStateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 暂停工作流并保存状态（不含对话历史）
     */
    public WorkflowPausedState pauseWorkflow(UUID sessionId,
                                              UUID workflowId,
                                              String subChainId,
                                              String llmNodeId,
                                              String pauseReason,
                                              WorkflowContext context,
                                              UUID pendingToolId,
                                              String pendingToolName,
                                              Map<String, Object> collectedParams,
                                              int currentRound,
                                              int maxRounds,
                                              String nextQuestion) {
        return pauseWorkflow(sessionId, workflowId, subChainId, llmNodeId, pauseReason,
                context, pendingToolId, pendingToolName, collectedParams, 
                currentRound, maxRounds, nextQuestion, null);
    }

    /**
     * 暂停工作流并保存状态（含对话历史）
     * 
     * @param chatHistoryJson LLM 对话历史 JSON，用于恢复时继续对话
     */
    public WorkflowPausedState pauseWorkflow(UUID sessionId,
                                              UUID workflowId,
                                              String subChainId,
                                              String llmNodeId,
                                              String pauseReason,
                                              WorkflowContext context,
                                              UUID pendingToolId,
                                              String pendingToolName,
                                              Map<String, Object> collectedParams,
                                              int currentRound,
                                              int maxRounds,
                                              String nextQuestion,
                                              String chatHistoryJson) {
        try {
            // 先取消该会话之前的所有未完成暂停状态
            cancelPendingStates(sessionId);

            WorkflowPausedState state = new WorkflowPausedState();
            state.setSessionId(sessionId);
            state.setWorkflowId(workflowId);
            state.setSubChainId(subChainId);
            state.setLlmNodeId(llmNodeId);
            state.setPauseReason(pauseReason);
            state.setPendingToolId(pendingToolId);
            state.setPendingToolName(pendingToolName);
            state.setCurrentRound(currentRound);
            state.setMaxRounds(maxRounds);
            state.setNextQuestion(nextQuestion);
            state.setStatus(WorkflowPausedState.Status.WAITING_USER_INPUT);

            // 序列化上下文
            if (context != null) {
                state.setContextJson(serializeContext(context));
            }

            // 序列化已收集的参数
            if (collectedParams != null && !collectedParams.isEmpty()) {
                state.setCollectedParamsJson(objectMapper.writeValueAsString(collectedParams));
            }

            // 保存对话历史
            if (chatHistoryJson != null && !chatHistoryJson.isEmpty()) {
                state.setChatHistoryJson(chatHistoryJson);
                log.debug("保存对话历史: length={}", chatHistoryJson.length());
            }

            // 设置过期时间（30分钟）
            state.setExpiredAt(Instant.now().plusSeconds(30 * 60));

            WorkflowPausedState saved = pausedStateRepository.save(state);
            
            log.info("工作流已暂停: sessionId={}, workflowId={}, subChainId={}, llmNodeId={}, toolName={}, round={}/{}, hasHistory={}",
                    sessionId, workflowId, subChainId, llmNodeId, pendingToolName, currentRound, maxRounds, chatHistoryJson != null);

            return saved;
        } catch (Exception e) {
            log.error("保存暂停状态失败", e);
            throw new RuntimeException("保存暂停状态失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找会话的未完成暂停状态
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowPausedState> findPendingState(UUID sessionId) {
        // 先标记过期的状态
        pausedStateRepository.markExpiredStates(Instant.now());
        return pausedStateRepository.findLatestPendingBySessionId(sessionId);
    }

    /**
     * 恢复工作流执行
     */
    public void resumeWorkflow(UUID pausedStateId) {
        pausedStateRepository.findById(pausedStateId).ifPresent(state -> {
            state.setStatus(WorkflowPausedState.Status.RESUMED);
            pausedStateRepository.save(state);
            log.info("工作流状态已更新为已恢复: id={}", pausedStateId);
        });
    }

    /**
     * 标记工作流完成
     */
    public void completeWorkflow(UUID pausedStateId) {
        pausedStateRepository.findById(pausedStateId).ifPresent(state -> {
            state.setStatus(WorkflowPausedState.Status.COMPLETED);
            pausedStateRepository.save(state);
            log.info("工作流状态已更新为已完成: id={}", pausedStateId);
        });
    }

    /**
     * 取消会话的所有未完成暂停状态
     */
    public void cancelPendingStates(UUID sessionId) {
        int count = pausedStateRepository.cancelAllPendingBySessionId(sessionId, Instant.now());
        if (count > 0) {
            log.info("已取消 {} 个未完成的暂停状态: sessionId={}", count, sessionId);
        }
    }

    /**
     * 更新暂停状态（收集到更多参数后）
     */
    public void updatePausedState(UUID pausedStateId,
                                   Map<String, Object> collectedParams,
                                   int currentRound,
                                   String nextQuestion) {
        pausedStateRepository.findById(pausedStateId).ifPresent(state -> {
            try {
                if (collectedParams != null) {
                    state.setCollectedParamsJson(objectMapper.writeValueAsString(collectedParams));
                }
                state.setCurrentRound(currentRound);
                state.setNextQuestion(nextQuestion);
                pausedStateRepository.save(state);
                log.info("更新暂停状态: id={}, round={}", pausedStateId, currentRound);
            } catch (Exception e) {
                log.error("更新暂停状态失败", e);
            }
        });
    }

    /**
     * 获取已收集的参数
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCollectedParams(UUID pausedStateId) {
        return pausedStateRepository.findById(pausedStateId)
                .map(state -> {
                    try {
                        if (state.getCollectedParamsJson() != null) {
                            return objectMapper.readValue(
                                    state.getCollectedParamsJson(),
                                    new TypeReference<Map<String, Object>>() {}
                            );
                        }
                    } catch (Exception e) {
                        log.error("解析已收集参数失败", e);
                    }
                    return new HashMap<String, Object>();
                })
                .orElse(new HashMap<>());
    }

    /**
     * 序列化上下文（只保存关键数据）
     */
    private String serializeContext(WorkflowContext context) {
        try {
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("workflowId", context.getWorkflowId() != null ? context.getWorkflowId().toString() : null);
            contextData.put("sessionId", context.getSessionId() != null ? context.getSessionId().toString() : null);
            contextData.put("query", context.getQuery());
            contextData.put("variables", context.getVariables());
            contextData.put("nodeOutputs", context.getNodeOutputs());
            contextData.put("finalReply", context.getFinalReply());
            return objectMapper.writeValueAsString(contextData);
        } catch (Exception e) {
            log.error("序列化上下文失败", e);
            return null;
        }
    }

    /**
     * 反序列化上下文
     */
    public WorkflowContext deserializeContext(String contextJson, UUID workflowId, UUID sessionId, String newQuery) {
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId(workflowId);
        context.setSessionId(sessionId);
        context.setQuery(newQuery);
        
        if (contextJson != null) {
            try {
                Map<String, Object> contextData = objectMapper.readValue(
                        contextJson,
                        new TypeReference<Map<String, Object>>() {}
                );
                
                // 恢复变量
                if (contextData.get("variables") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> variables = (Map<String, Object>) contextData.get("variables");
                    variables.forEach(context::setVariable);
                }
                
                // 恢复节点输出
                if (contextData.get("nodeOutputs") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nodeOutputs = (Map<String, Object>) contextData.get("nodeOutputs");
                    nodeOutputs.forEach((k, v) -> context.setNodeOutput(k, v != null ? v.toString() : null));
                }
                
            } catch (Exception e) {
                log.error("反序列化上下文失败", e);
            }
        }
        
        return context;
    }

    /**
     * 清理旧的暂停状态（保留7天）
     */
    public void cleanupOldStates() {
        Instant before = Instant.now().minusSeconds(7 * 24 * 60 * 60);
        int count = pausedStateRepository.deleteOldStates(before);
        if (count > 0) {
            log.info("已清理 {} 个旧的暂停状态", count);
        }
    }
}

