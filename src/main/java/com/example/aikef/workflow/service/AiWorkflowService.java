package com.example.aikef.workflow.service;

import com.example.aikef.dto.AiWorkflowDto;
import com.example.aikef.dto.request.SaveWorkflowRequest;
import com.example.aikef.model.AiWorkflow;
import com.example.aikef.model.Agent;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.WorkflowCategoryBinding;
import com.example.aikef.model.WorkflowExecutionLog;
import com.example.aikef.model.SessionCategory;
import com.example.aikef.repository.AiWorkflowRepository;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.SessionCategoryRepository;
import com.example.aikef.repository.WorkflowCategoryBindingRepository;
import com.example.aikef.repository.WorkflowExecutionLogRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.service.AgentService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.converter.ReactFlowToLiteflowConverter;
import com.example.aikef.workflow.dto.WorkflowEdgeDto;
import com.example.aikef.workflow.dto.WorkflowNodeDto;
import com.example.aikef.workflow.model.WorkflowPausedState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * AI 工作流服务
 */
@Service
@Transactional(readOnly = true)
public class AiWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AiWorkflowService.class);

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private AiWorkflowRepository workflowRepository;

    @Resource
    private WorkflowExecutionLogRepository executionLogRepository;

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private AgentService agentService;

    @Resource
    private ReactFlowToLiteflowConverter converter;

    @Resource
    private WorkflowPauseService pauseService;

    @Resource
    private WorkflowCategoryBindingRepository categoryBindingRepository;

    @Resource
    private SessionCategoryRepository sessionCategoryRepository;

    @Resource
    private com.example.aikef.repository.AgentSessionRepository agentSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== CRUD 操作 ====================

    /**
     * 创建工作流
     * 支持创建空模板（没有节点）
     * 自动拆分 LLM 节点及其后续流程为子链
     */
    @Transactional
    public AiWorkflow createWorkflow(SaveWorkflowRequest request, UUID agentId) {
        // 验证名称唯一性
        if (workflowRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("工作流名称已存在: " + request.name());
        }

        // 检查是否为空模板
        boolean isEmptyTemplate = isEmptyWorkflow(request.nodesJson());
        
        String liteflowEl = null;
        
        if (!isEmptyTemplate) {
            // 非空模板：验证工作流结构
            var validation = converter.validate(request.nodesJson(), request.edgesJson());
            if (!validation.valid()) {
                throw new IllegalArgumentException("工作流结构无效: " + String.join(", ", validation.errors()));
            }
            
            // 转换为 LiteFlow EL 表达式（不使用子链拆分）
            liteflowEl = converter.convert(request.nodesJson(), request.edgesJson());
            log.info("========== 生成的 EL 表达式 ==========\n{}", liteflowEl);
        }

        AiWorkflow workflow = new AiWorkflow();
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setNodesJson(request.nodesJson() != null ? request.nodesJson() : "[]");
        workflow.setEdgesJson(request.edgesJson() != null ? request.edgesJson() : "[]");
        workflow.setLiteflowEl(liteflowEl);
        workflow.setEnabled(true); // 默认启用
        workflow.setVersion(1);
        workflow.setTriggerType(request.triggerType() != null ? request.triggerType() : "ALL");
        workflow.setTriggerConfig(request.triggerConfig());

        if (agentId != null) {
            Agent agent = agentService.findById(agentId);
            workflow.setCreatedByAgent(agent);
        }

        AiWorkflow saved = workflowRepository.save(workflow);
        
        // 绑定分类
        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            bindWorkflowToCategories(saved.getId(), request.categoryIds());
        }
        
        // 注册子链到 LiteFlow
        return saved;
    }
    
    /**
     * 复制工作流
     * 创建一个新的工作流，复制原工作流的所有配置（除了绑定的分类）
     * 新工作流名称自动添加 "_copy" 后缀
     */
    @Transactional
    public AiWorkflow copyWorkflow(UUID workflowId, UUID agentId) {
        AiWorkflow original = getWorkflow(workflowId);
        
        AiWorkflow copy = new AiWorkflow();
        // 生成唯一名称
        String newName = original.getName() + "_copy";
        while (workflowRepository.existsByName(newName)) {
            newName += "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        
        copy.setName(newName);
        copy.setDescription(original.getDescription());
        copy.setNodesJson(original.getNodesJson());
        copy.setEdgesJson(original.getEdgesJson());
        copy.setLiteflowEl(original.getLiteflowEl());
        copy.setSubChainsJson(original.getSubChainsJson());
        copy.setEnabled(false); // 复制的工作流默认禁用
        copy.setVersion(1);
        copy.setTriggerType(original.getTriggerType());
        copy.setTriggerConfig(original.getTriggerConfig());
        copy.setIsDefault(false); // 复制的工作流不作为默认

        if (agentId != null) {
            Agent agent = agentService.findById(agentId);
            copy.setCreatedByAgent(agent);
        }

        return workflowRepository.save(copy);
    }

    /**
     * 检查是否为空工作流
     */
    private boolean isEmptyWorkflow(String nodesJson) {
        if (nodesJson == null || nodesJson.isEmpty()) {
            return true;
        }
        try {
            List<?> nodes = objectMapper.readValue(nodesJson, List.class);
            return nodes.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 注册子链到 LiteFlow
     * 将 LLM 节点拆分的子链动态注册，支持工作流暂停/恢复
     * 按依赖顺序注册：不引用其他子链的先注册
     */
    private void registerSubChains(AiWorkflow workflow) {
        if (workflow.getSubChainsJson() == null || workflow.getSubChainsJson().isEmpty()) {
            return;
        }
        
        try {
            // 解析子链信息
            Map<String, Map<String, Object>> subChains = objectMapper.readValue(
                    workflow.getSubChainsJson(),
                    new TypeReference<Map<String, Map<String, Object>>>() {}
            );
            
            // 收集所有子链 ID
            Set<String> allChainIds = new HashSet<>();
            for (Map<String, Object> info : subChains.values()) {
                String chainId = (String) info.get("chainId");
                if (chainId != null) {
                    allChainIds.add(chainId);
                }
            }
            
            // 按依赖排序：不引用其他子链的先注册
            List<Map.Entry<String, Map<String, Object>>> sortedEntries = new ArrayList<>(subChains.entrySet());
            sortedEntries.sort((a, b) -> {
                String elA = (String) a.getValue().get("chainEl");
                String elB = (String) b.getValue().get("chainEl");
                return Integer.compare(countSubChainRefs(elA, allChainIds), countSubChainRefs(elB, allChainIds));
            });
            
            // 已注册的子链
            Set<String> registered = new HashSet<>();
            
            // 多轮注册，处理依赖
            int maxRounds = subChains.size() + 1;
            for (int round = 0; round < maxRounds && registered.size() < subChains.size(); round++) {
                for (Map.Entry<String, Map<String, Object>> entry : sortedEntries) {
                    String llmNodeId = entry.getKey();
                    Map<String, Object> subChainInfo = entry.getValue();
                    
                    String chainId = (String) subChainInfo.get("chainId");
                    String chainEl = (String) subChainInfo.get("chainEl");
                    
                    if (chainId == null || registered.contains(chainId)) {
                        continue;
                    }
                    
                    if (chainEl == null || chainEl.isBlank()) {
                        log.warn("子链 EL 为空，跳过: chainId={}", chainId);
                        registered.add(chainId);
                        continue;
                    }
                    
                    // 检查依赖的子链是否都已注册
                    boolean canRegister = true;
                    for (String otherChainId : allChainIds) {
                        if (!otherChainId.equals(chainId) && chainEl.contains(otherChainId) && !registered.contains(otherChainId)) {
                            canRegister = false;
                            break;
                        }
                    }
                    
                    if (canRegister) {
                        try {
                            LiteFlowChainELBuilder.createChain()
                                    .setChainId(chainId)
                                    .setEL(chainEl)
                                    .build();
                            
                            registered.add(chainId);
                            log.info("注册子链成功: chainId={}, llmNodeId={}", chainId, llmNodeId);
                        } catch (Exception e) {
                            log.warn("注册子链失败: chainId={}, error={}", chainId, e.getMessage());
                            registered.add(chainId);  // 避免死循环
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析子链信息失败: workflowId={}", workflow.getId(), e);
        }
    }
    
    /**
     * 计算 EL 表达式中引用的其他子链数量
     */
    private int countSubChainRefs(String el, Set<String> allChainIds) {
        if (el == null) return 0;
        int count = 0;
        for (String chainId : allChainIds) {
            if (el.contains(chainId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取子链信息
     */
    public Map<String, ReactFlowToLiteflowConverter.SubChainInfo> getSubChains(UUID workflowId) {
        AiWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("工作流不存在"));
        
        if (workflow.getSubChainsJson() == null || workflow.getSubChainsJson().isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            return objectMapper.readValue(
                    workflow.getSubChainsJson(),
                    new TypeReference<Map<String, ReactFlowToLiteflowConverter.SubChainInfo>>() {}
            );
        } catch (Exception e) {
            log.error("解析子链信息失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 执行指定子链（用于工作流恢复）
     */
    public WorkflowExecutionResult executeSubChain(String chainId, WorkflowContext context) {
        try {
            log.info("执行子链: chainId={}", chainId);
            
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, null, context);
            
            // 检查是否因暂停而中断
            if (!response.isSuccess()) {
                Throwable cause = response.getCause();
                if (cause instanceof com.example.aikef.workflow.exception.WorkflowPausedException pauseEx) {
                    log.info("子链执行暂停: reason={}, message={}", pauseEx.getPauseReason(), pauseEx.getPauseMessage());
                    return new WorkflowExecutionResult(true, pauseEx.getPauseMessage(), null, null, false);
                }
                return new WorkflowExecutionResult(false, null, response.getMessage(), null, false);
            }
            
            // 检查上下文暂停标记
            if (context.isPaused()) {
                log.info("子链执行暂停: reason={}, message={}", context.getPauseReason(), context.getPauseMessage());
                return new WorkflowExecutionResult(true, context.getPauseMessage(), null, null, false);
            }
            
            return new WorkflowExecutionResult(
                    true,
                    context.getFinalReply(),
                    null,
                    null,
                    context.isNeedHumanTransfer()
            );
        } catch (com.example.aikef.workflow.exception.WorkflowPausedException pauseEx) {
            log.info("子链执行暂停: reason={}, message={}", pauseEx.getPauseReason(), pauseEx.getPauseMessage());
            return new WorkflowExecutionResult(true, pauseEx.getPauseMessage(), null, null, false);
        } catch (Exception e) {
            log.error("执行子链失败: chainId={}", chainId, e);
            return new WorkflowExecutionResult(false, null, e.getMessage(), null, false);
        }
    }

    /**
     * 更新工作流
     * 自动拆分 LLM 节点及其后续流程为子链
     */
    @Transactional
    public AiWorkflow updateWorkflow(UUID workflowId, SaveWorkflowRequest request) {
        AiWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("工作流不存在"));

        // 检查名称是否与其他工作流冲突
        workflowRepository.findByName(request.name())
                .filter(w -> !w.getId().equals(workflowId))
                .ifPresent(w -> {
                    throw new IllegalArgumentException("工作流名称已存在: " + request.name());
                });

        // 检查是否为空模板
        boolean isEmptyTemplate = isEmptyWorkflow(request.nodesJson());
        
        String liteflowEl = null;
        
        if (!isEmptyTemplate) {
            // 验证工作流结构
            var validation = converter.validate(request.nodesJson(), request.edgesJson());
            if (!validation.valid()) {
                throw new IllegalArgumentException("工作流结构无效: " + String.join(", ", validation.errors()));
            }

            // 转换为 LiteFlow EL 表达式（不使用子链拆分）
            liteflowEl = converter.convert(request.nodesJson(), request.edgesJson());
            log.info("========== [更新] 生成的 EL 表达式 ==========\n{}", liteflowEl);
        }

        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setNodesJson(request.nodesJson() != null ? request.nodesJson() : "[]");
        workflow.setEdgesJson(request.edgesJson() != null ? request.edgesJson() : "[]");
        workflow.setLiteflowEl(liteflowEl);
        workflow.setVersion(workflow.getVersion() + 1);

        if (request.triggerType() != null) {
            workflow.setTriggerType(request.triggerType());
        }
        if (request.triggerConfig() != null) {
            workflow.setTriggerConfig(request.triggerConfig());
        }

        AiWorkflow saved = workflowRepository.save(workflow);
        
        // 更新分类绑定（如果提供了 categoryIds）
        if (request.categoryIds() != null) {
            bindWorkflowToCategories(saved.getId(), request.categoryIds());
        }
        
        // 注册子链到 LiteFlow
        return saved;
    }

    /**
     * 删除工作流
     */
    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new EntityNotFoundException("工作流不存在");
        }
        
        // 删除分类绑定
        categoryBindingRepository.deleteByWorkflow_Id(workflowId);
        workflowRepository.deleteById(workflowId);
    }

    /**
     * 获取工作流详情
     */
    public AiWorkflow getWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("工作流不存在"));
    }

    /**
     * 获取所有工作流
     */
    public List<AiWorkflow> getAllWorkflows() {
        return workflowRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 启用/禁用工作流
     */
    @Transactional
    public AiWorkflow toggleWorkflow(UUID workflowId, boolean enabled) {
        AiWorkflow workflow = getWorkflow(workflowId);
        workflow.setEnabled(enabled);
        return workflowRepository.save(workflow);
    }

    /**
     * 设置默认工作流
     */
    @Transactional
    public AiWorkflow setDefaultWorkflow(UUID workflowId) {
        // 取消原有默认工作流
        workflowRepository.findByIsDefaultTrueAndEnabledTrue()
                .ifPresent(w -> {
                    w.setIsDefault(false);
                    workflowRepository.save(w);
                });

        AiWorkflow workflow = getWorkflow(workflowId);
        workflow.setIsDefault(true);
        workflow.setEnabled(true);
        return workflowRepository.save(workflow);
    }

    // ==================== 执行工作流 ====================

    /**
     * 执行工作流
     */
    @Transactional
    public WorkflowExecutionResult executeWorkflow(UUID workflowId, UUID sessionId, 
                                                    String userMessage,
                                                    Map<String, Object> variables) {
        AiWorkflow workflow = getWorkflow(workflowId);
        return executeWorkflowInternal(workflow, sessionId, userMessage, variables, null);
    }

    @Value("${app.saas.enabled:false}")
    private boolean saasEnabled;

    /**
     * 根据会话自动选择并执行工作流
     * 优先级：
     * 1. 工具询问（检查 WorkflowPausedState）
     * 2. Agent 工作流（检查 AgentSession）
     * 3. 分类绑定工作流
     * 
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param messageId 触发工作流的消息ID（可为null）
     */
    @Transactional
    public WorkflowExecutionResult executeForSession(UUID sessionId, String userMessage, UUID messageId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));

        if(saasEnabled){
            try {
                if (session != null && session.getTenantId() != null) {
                    TenantContext.setTenantId(session.getTenantId());
                    log.debug("WebSocket: 已为会话 {} 设置租户上下文: {}", sessionId, session.getTenantId());
                } else {
                    log.warn("WebSocket: 无法获取会话 {} 的租户信息", sessionId);
                }
            } catch (Exception e) {
                log.error("WebSocket: 设置租户上下文失败: sessionId={}, error={}", sessionId, e.getMessage());
            }
        }


        // 优先级1: 检查是否有未完成的暂停状态（工具询问等）
        Optional<WorkflowPausedState> pausedStateOpt = pauseService.findPendingState(sessionId);
        if (pausedStateOpt.isPresent()) {
            WorkflowPausedState pausedState = pausedStateOpt.get();
            log.info("发现未完成的暂停状态，恢复执行: sessionId={}, subChainId={}, llmNodeId={}",
                    sessionId, pausedState.getSubChainId(), pausedState.getLlmNodeId());
            return resumeFromPausedState(pausedState, userMessage, session, messageId);
        }

        // 优先级2: 检查是否有未结束的 AgentSession（特殊工作流）
        Optional<com.example.aikef.model.AgentSession> agentSessionOpt = 
                agentSessionRepository.findBySessionIdAndNotEnded(sessionId);
        if (agentSessionOpt.isPresent()) {
            com.example.aikef.model.AgentSession agentSession = agentSessionOpt.get();
            AiWorkflow agentWorkflow = agentSession.getWorkflow();
            
            log.info("发现未结束的 AgentSession，执行 Agent 工作流: sessionId={}, workflowId={}, workflowName={}",
                    sessionId, agentWorkflow.getId(), agentWorkflow.getName());
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("sessionId", sessionId);
            if (session.getCustomer() != null) {
                variables.put("customerId", session.getCustomer().getId());
                variables.put("customerName", session.getCustomer().getName());
            }
            if (session.getCategory() != null) {
                variables.put("categoryId", session.getCategory().getId());
                variables.put("categoryName", session.getCategory().getName());
            }

            return executeWorkflowInternalWithAgentSession(agentWorkflow, sessionId, userMessage, variables, agentSession, messageId);
        }

        // 优先级3: 根据会话分类查找匹配的工作流
        AiWorkflow workflow = findWorkflowForSession(session);
        if (workflow == null) {
            log.warn("未找到匹配的工作流: sessionId={}, categoryId={}", 
                    sessionId, session.getCategory() != null ? session.getCategory().getId() : null);
            return new WorkflowExecutionResult(false, null, "未找到匹配的工作流", null, null);
        }

        log.info("会话匹配工作流: sessionId={}, workflowId={}, workflowName={}", 
                sessionId, workflow.getId(), workflow.getName());

        Map<String, Object> variables = new HashMap<>();
        variables.put("sessionId", sessionId);
        if (session.getCustomer() != null) {
            variables.put("customerId", session.getCustomer().getId());
            variables.put("customerName", session.getCustomer().getName());
        }
        if (session.getCategory() != null) {
            variables.put("categoryId", session.getCategory().getId());
            variables.put("categoryName", session.getCategory().getName());
        }

        return executeWorkflowInternal(workflow, sessionId, userMessage, variables, messageId);
    }

    /**
     * 执行 Agent 工作流（带 AgentSession）
     * 供 AgentNode 调用，立即执行目标工作流
     */
    public WorkflowExecutionResult executeWorkflowInternalWithAgentSession(
            AiWorkflow workflow, UUID sessionId, String userMessage,
            Map<String, Object> variables, com.example.aikef.model.AgentSession agentSession, UUID messageId) {
        long startTime = System.currentTimeMillis();
        WorkflowExecutionLog log = new WorkflowExecutionLog();
        log.setWorkflow(workflow);
        log.setUserInput(userMessage);
        log.setStartedAt(Instant.now());

        if (sessionId != null) {
            chatSessionRepository.findById(sessionId).ifPresent(log::setSession);
        }

        try {
            WorkflowExecutionResult result = executeWorkflowInternalWithoutLog(
                    workflow, sessionId, userMessage, variables, agentSession, messageId);

            log.setStatus(result.success() ? "SUCCESS" : "FAILED");
            log.setFinalOutput(result.reply());
            log.setNodeDetails(result.nodeDetailsJson());
            log.setErrorMessage(result.errorMessage());
            log.setFinishedAt(Instant.now());
            log.setDurationMs(System.currentTimeMillis() - startTime);

            executionLogRepository.save(log);
            return result;

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setFinishedAt(Instant.now());
            log.setDurationMs(System.currentTimeMillis() - startTime);
            executionLogRepository.save(log);

            return new WorkflowExecutionResult(false, null, e.getMessage(), null, false);
        }
    }

    /**
     * 根据会话查找匹配的工作流
     * 优先级：
     * 1. 会话分类绑定的工作流
     * 2. 默认工作流
     */
    private AiWorkflow findWorkflowForSession(ChatSession session) {
        // 1. 如果会话有分类，查找绑定的工作流
        if (session.getCategory() != null) {

            log.info("categoryId={}", session.getCategory().getId());
            Optional<WorkflowCategoryBinding> bindingOpt = categoryBindingRepository
                    .findByCategoryIdWithWorkflow(session.getCategory().getId());
            
            if (bindingOpt.isPresent()) {
                return bindingOpt.get().getWorkflow();
            }
        }


        // 2. 查找默认工作流
        return workflowRepository.findByIsDefaultTrueAndEnabledTrue().orElse(null);
    }

    /**
     * 根据分类ID查找绑定的工作流（一个分类只能绑定一个工作流）
     */
    public Optional<AiWorkflow> findWorkflowByCategoryId(UUID categoryId) {
        return categoryBindingRepository.findByCategoryIdWithWorkflow(categoryId)
                .map(WorkflowCategoryBinding::getWorkflow);
    }

    /**
     * 绑定工作流到分类
     * 一个工作流可以绑定多个分类，一个分类只能绑定一个工作流
     */
    @Transactional
    public void bindWorkflowToCategories(UUID workflowId, List<UUID> categoryIds) {
        AiWorkflow workflow = getWorkflow(workflowId);
        
        // 检查分类是否已被其他工作流绑定
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (UUID categoryId : categoryIds) {
                if (categoryBindingRepository.existsByCategoryIdAndWorkflowIdNot(categoryId, workflowId)) {
                    SessionCategory category = sessionCategoryRepository.findById(categoryId).orElse(null);
                    String categoryName = category != null ? category.getName() : categoryId.toString();
                    throw new IllegalArgumentException("分类 [" + categoryName + "] 已被其他工作流绑定");
                }
            }
        }
        
        // 删除旧绑定（@Modifying 注解已配置自动刷新）
        categoryBindingRepository.deleteByWorkflow_Id(workflowId);
        
        // 创建新绑定
        if (categoryIds != null && !categoryIds.isEmpty()) {
            int priority = 0;
            for (UUID categoryId : categoryIds) {
                SessionCategory category = sessionCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException("分类不存在: " + categoryId));
                
                WorkflowCategoryBinding binding = new WorkflowCategoryBinding();
                binding.setWorkflow(workflow);
                binding.setCategory(category);
                binding.setPriority(priority++);
                categoryBindingRepository.save(binding);
            }
        }
        
        log.info("工作流分类绑定更新: workflowId={}, categories={}", workflowId, categoryIds);
    }

    /**
     * 获取工作流绑定的分类ID列表
     */
    public List<UUID> getWorkflowCategoryIds(UUID workflowId) {
        List<WorkflowCategoryBinding> bindings = categoryBindingRepository.findByWorkflowIdWithCategory(workflowId);
        return bindings.stream()
                .map(b -> b.getCategory().getId())
                .toList();
    }

    /**
     * 获取工作流绑定的分类详情
     */
    public List<SessionCategory> getWorkflowCategories(UUID workflowId) {
        List<WorkflowCategoryBinding> bindings = categoryBindingRepository.findByWorkflowIdWithCategory(workflowId);
        return bindings.stream()
                .map(WorkflowCategoryBinding::getCategory)
                .toList();
    }

    /**
     * 获取当前工作流可绑定的分类列表
     * 返回未被其他工作流绑定的分类 + 当前工作流已绑定的分类
     */
    public List<SessionCategory> getAvailableCategoriesForWorkflow(UUID workflowId) {
        // 获取所有启用的分类
        List<SessionCategory> allCategories = sessionCategoryRepository.findByEnabledTrueOrderBySortOrderAsc();
        
        // 获取已被其他工作流绑定的分类ID（排除当前工作流）
        List<UUID> boundByOthers = categoryBindingRepository.findBoundCategoryIdsExcludingWorkflow(workflowId);
        
        // 过滤出可用的分类
        return allCategories.stream()
                .filter(c -> !boundByOthers.contains(c.getId()))
                .toList();
    }

    /**
     * 获取所有可绑定的分类列表（用于新建工作流）
     * 返回未被任何工作流绑定的分类
     */
    public List<SessionCategory> getAvailableCategories() {
        // 获取所有启用的分类
        List<SessionCategory> allCategories = sessionCategoryRepository.findByEnabledTrueOrderBySortOrderAsc();
        
        // 获取已被绑定的分类ID
        List<UUID> boundCategoryIds = categoryBindingRepository.findAllBoundCategoryIds();
        
        // 过滤出可用的分类
        return allCategories.stream()
                .filter(c -> !boundCategoryIds.contains(c.getId()))
                .toList();
    }

    /**
     * 从暂停状态恢复执行工作流
     */
    @Transactional
    public WorkflowExecutionResult resumeFromPausedState(WorkflowPausedState pausedState, 
                                                          String userMessage,
                                                          ChatSession session,
                                                          UUID messageId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取工作流
            AiWorkflow workflow = workflowRepository.findById(pausedState.getWorkflowId())
                    .orElseThrow(() -> new EntityNotFoundException("工作流不存在"));

            // 恢复上下文
            WorkflowContext context = pauseService.deserializeContext(
                    pausedState.getContextJson(),
                    pausedState.getWorkflowId(),
                    pausedState.getSessionId(),
                    userMessage
            );
            
            // 设置触发工作流的消息ID
            context.setMessageId(messageId);

            // 解析并设置节点配置（重要：子链执行时 LLM 节点需要这个配置）
            Map<String, JsonNode> nodesConfig = parseNodesConfig(workflow.getNodesJson());
            context.setNodesConfig(nodesConfig);
            
            // 解析节点标签映射
            Map<String, String> nodeLabels = parseNodeLabels(workflow.getNodesJson());
            context.setNodeLabels(nodeLabels);
            
            // 从边数据中提取意图路由映射
            extractIntentRoutesFromEdges(workflow.getNodesJson(), workflow.getEdgesJson(), context);
            
            // 从边数据中提取工具节点路由映射
            extractToolRoutesFromEdges(workflow.getNodesJson(), workflow.getEdgesJson(), context);

            // 从边数据中提取条件节点路由映射
            extractConditionRoutesFromEdges(workflow.getNodesJson(), workflow.getEdgesJson(), context);

            // 设置恢复相关的上下文信息
            context.setVariable("_resumeFromPause", true);
            context.setVariable("_pausedStateId", pausedState.getId());
            context.setVariable("_pendingToolId", pausedState.getPendingToolId());
            context.setVariable("_pendingToolName", pausedState.getPendingToolName());
            context.setVariable("_currentRound", pausedState.getCurrentRound());
            context.setVariable("_maxRounds", pausedState.getMaxRounds());

            // 恢复已收集的参数
            Map<String, Object> collectedParams = pauseService.getCollectedParams(pausedState.getId());
            context.setVariable("_collectedParams", collectedParams);

            // 恢复保存的对话历史（用于 LLM 节点继续对话）
            if (pausedState.getChatHistoryJson() != null && !pausedState.getChatHistoryJson().isEmpty()) {
                try {
                    List<Map<String, String>> savedChatHistory = objectMapper.readValue(
                            pausedState.getChatHistoryJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {}
                    );
                    context.setVariable("_savedChatHistory", savedChatHistory);
                    log.info("恢复对话历史: {} 条消息", savedChatHistory.size());
                } catch (Exception e) {
                    log.error("解析对话历史失败", e);
                }
            }

            // 标记暂停状态为已恢复
            pauseService.resumeWorkflow(pausedState.getId());

            log.info("从暂停状态恢复执行子链: subChainId={}, userMessage={}", 
                    pausedState.getSubChainId(), userMessage);

            // 执行子链
            LiteflowResponse response = flowExecutor.execute2Resp(
                    pausedState.getSubChainId(), null, context);

            // 检查是否因暂停而中断
            if (!response.isSuccess()) {
                Throwable cause = response.getCause();
                if (cause instanceof com.example.aikef.workflow.exception.WorkflowPausedException pauseEx) {
                    log.info("工作流再次暂停: reason={}, message={}", 
                            pauseEx.getPauseReason(), pauseEx.getPauseMessage());
                    return new WorkflowExecutionResult(true, pauseEx.getPauseMessage(), null, null, false);
                }
                log.error("子链执行失败: subChainId={}, error={}",
                        pausedState.getSubChainId(), response.getMessage());
                return new WorkflowExecutionResult(false, null, response.getMessage(), null, false);
            }

            // 检查上下文是否标记为暂停
            if (context.isPaused()) {
                log.info("工作流再次暂停: reason={}, message={}", 
                        context.getPauseReason(), context.getPauseMessage());
                return new WorkflowExecutionResult(true, context.getPauseMessage(), null, null, false);
            }

            // 执行成功，标记暂停状态为已完成
            pauseService.completeWorkflow(pausedState.getId());

            log.info("子链执行成功: subChainId={}, duration={}ms",
                    pausedState.getSubChainId(), System.currentTimeMillis() - startTime);
            return new WorkflowExecutionResult(
                    true,
                    context.getFinalReply(),
                    null,
                    null,
                    context.isNeedHumanTransfer()
            );
        } catch (com.example.aikef.workflow.exception.WorkflowPausedException pauseEx) {
            log.info("工作流再次暂停: reason={}, message={}", pauseEx.getPauseReason(), pauseEx.getPauseMessage());
            return new WorkflowExecutionResult(true, pauseEx.getPauseMessage(), null, null, false);
        } catch (Exception e) {
            log.error("从暂停状态恢复执行失败", e);
            return new WorkflowExecutionResult(false, null, e.getMessage(), null, false);
        }
    }

    /**
     * 取消会话的暂停状态
     */
    @Transactional
    public void cancelPausedState(UUID sessionId) {
        pauseService.cancelPendingStates(sessionId);
    }

    /**
     * 检查会话是否有暂停状态
     */
    @Transactional(readOnly = true)
    public boolean hasPausedState(UUID sessionId) {
        return pauseService.findPendingState(sessionId).isPresent();
    }

    /**
     * 获取会话的暂停状态信息
     */
    @Transactional(readOnly = true)
    public Optional<PausedStateInfo> getPausedStateInfo(UUID sessionId) {
        return pauseService.findPendingState(sessionId)
                .map(state -> new PausedStateInfo(
                        state.getId(),
                        state.getWorkflowId(),
                        state.getSubChainId(),
                        state.getLlmNodeId(),
                        state.getPendingToolName(),
                        state.getNextQuestion(),
                        state.getCurrentRound(),
                        state.getMaxRounds(),
                        state.getCreatedAt(),
                        state.getExpiredAt()
                ));
    }

    /**
     * 暂停状态信息 DTO
     */
    public record PausedStateInfo(
            UUID id,
            UUID workflowId,
            String subChainId,
            String llmNodeId,
            String pendingToolName,
            String nextQuestion,
            Integer currentRound,
            Integer maxRounds,
            Instant createdAt,
            Instant expiredAt
    ) {}

    /**
     * 测试执行工作流（不保存日志）
     */
    public WorkflowExecutionResult testWorkflow(UUID workflowId, UUID sessionId, String userMessage,
                                                 Map<String, Object> variables) {
        AiWorkflow workflow = getWorkflow(workflowId);
        return executeWorkflowInternalWithoutLog(workflow, sessionId, userMessage, variables);
    }

    /**
     * 内部执行方法
     */
    private WorkflowExecutionResult executeWorkflowInternal(AiWorkflow workflow, UUID sessionId,
                                                            String userMessage,
                                                            Map<String, Object> variables, UUID messageId) {
        long startTime = System.currentTimeMillis();
        WorkflowExecutionLog log = new WorkflowExecutionLog();
        log.setWorkflow(workflow);
        log.setUserInput(userMessage);
        log.setStartedAt(Instant.now());

        if (sessionId != null) {
            chatSessionRepository.findById(sessionId).ifPresent(log::setSession);
        }

        try {
            WorkflowExecutionResult result = executeWorkflowInternalWithoutLog(
                    workflow, sessionId, userMessage, variables, null, messageId);

            log.setStatus(result.success() ? "SUCCESS" : "FAILED");
            log.setFinalOutput(result.reply());
            log.setNodeDetails(result.nodeDetailsJson());
            log.setErrorMessage(result.errorMessage());
            log.setFinishedAt(Instant.now());
            log.setDurationMs(System.currentTimeMillis() - startTime);

            executionLogRepository.save(log);
            return result;

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setFinishedAt(Instant.now());
            log.setDurationMs(System.currentTimeMillis() - startTime);
            executionLogRepository.save(log);

            return new WorkflowExecutionResult(false, null, e.getMessage(), null, false);
        }
    }

    /**
     * 执行工作流（不保存日志）
     */
    private WorkflowExecutionResult executeWorkflowInternalWithoutLog(AiWorkflow workflow, 
                                                                       UUID sessionId,
                                                                       String userMessage,
                                                                       Map<String, Object> variables) {
        return executeWorkflowInternalWithoutLog(workflow, sessionId, userMessage, variables, null, null);
    }

    /**
     * 执行工作流（不保存日志，支持 AgentSession）
     */
    private WorkflowExecutionResult executeWorkflowInternalWithoutLog(AiWorkflow workflow, 
                                                                       UUID sessionId,
                                                                       String userMessage,
                                                                       Map<String, Object> variables,
                                                                       com.example.aikef.model.AgentSession agentSession,
                                                                       UUID messageId) {
        // 为每个工作流生成唯一的 chain ID
        String chainId = "workflow_" + workflow.getId().toString().replace("-", "");
        
        try {
            // 构建执行上下文
            WorkflowContext context = new WorkflowContext();
            context.setWorkflowId(workflow.getId());
            context.setSessionId(sessionId);
            context.setQuery(userMessage);
            context.setMessageId(messageId);
            
            // 注入 AgentSession（如果存在）
            if (agentSession != null) {
                context.setAgentSession(agentSession);
                log.info("注入 AgentSession 到上下文: sessionId={}, workflowId={}, sysPrompt={}",
                        sessionId, workflow.getId(), agentSession.getSysPrompt());
            }
            
            if (variables != null) {
                context.setVariables(new HashMap<>(variables));
                if (variables.containsKey("customerId")) {
                    context.setCustomerId((UUID) variables.get("customerId"));
                }
            }

            // 解析节点配置
            Map<String, JsonNode> nodesConfig = parseNodesConfig(workflow.getNodesJson());
            context.setNodesConfig(nodesConfig);
            
            // 解析节点标签映射
            Map<String, String> nodeLabels = parseNodeLabels(workflow.getNodesJson());
            context.setNodeLabels(nodeLabels);
            
            // 从边数据中提取意图路由映射，注入到上下文
            extractIntentRoutesFromEdges(workflow.getNodesJson(), workflow.getEdgesJson(), context);
            
            // 从边数据中提取工具节点路由映射，注入到上下文
            extractToolRoutesFromEdges(workflow.getNodesJson(), workflow.getEdgesJson(), context);

            // 从边数据中提取条件节点路由映射，注入到上下文
            extractConditionRoutesFromEdges(workflow.getNodesJson(), workflow.getEdgesJson(), context);

            // 动态注册/更新 chain（使用 EL 表达式）
            String elExpression = workflow.getLiteflowEl();
            if (elExpression == null || elExpression.isBlank()) {
                return new WorkflowExecutionResult(false, null, "工作流 EL 表达式为空", null, false);
            }
            
            // 包装成 THEN 表达式确保是有效的 EL
            String wrappedEl = wrapElExpression(elExpression);
            
            log.debug("注册动态 chain: id={}, el={}", chainId, wrappedEl);
            LiteFlowChainELBuilder.createChain()
                    .setChainId(chainId)
                    .setEL(wrappedEl)
                    .build();

            // 执行工作流
            LiteflowResponse response = flowExecutor.execute2Resp(
                    chainId,
                    null,
                    context
            );

            // 检查是否因暂停而中断
            if (!response.isSuccess()) {
                Throwable cause = response.getCause();
                // 检查是否是暂停异常
                if (cause instanceof com.example.aikef.workflow.exception.WorkflowPausedException pauseEx) {
                    log.info("工作流暂停: reason={}, message={}", pauseEx.getPauseReason(), pauseEx.getPauseMessage());
                    return new WorkflowExecutionResult(true, pauseEx.getPauseMessage(), null, null, false);
                }
                String errorMsg = response.getMessage();
                if (cause != null) {
                    errorMsg = cause.getMessage();
                }
                return new WorkflowExecutionResult(false, null, errorMsg, null, false);
            }

            // 检查上下文是否标记为暂停
            if (context.isPaused()) {
                log.info("工作流暂停: reason={}, message={}", context.getPauseReason(), context.getPauseMessage());
                return new WorkflowExecutionResult(true, context.getPauseMessage(), null, null, false);
            }

            // 获取执行结果
            String reply = context.getFinalReply();
            String nodeDetailsJson = objectMapper.writeValueAsString(context.getNodeExecutionDetails());

            return new WorkflowExecutionResult(
                    true,
                    reply,
                    null,
                    nodeDetailsJson,
                    context.isNeedHumanTransfer()
            );

        } catch (com.example.aikef.workflow.exception.WorkflowPausedException pauseEx) {
            log.info("工作流暂停: reason={}, message={}", pauseEx.getPauseReason(), pauseEx.getPauseMessage());
            return new WorkflowExecutionResult(true, pauseEx.getPauseMessage(), null, null, false);
        } catch (Exception e) {
            AiWorkflowService.log.error("工作流执行失败", e);
            return new WorkflowExecutionResult(false, null, e.getMessage(), null, false);
        }
    }
    
    /**
     * 包装 EL 表达式，确保是有效的 LiteFlow EL
     * 如果表达式不是以 THEN/WHEN/IF/SWITCH 等关键字开头，则包装成 THEN(...)
     */
    private String wrapElExpression(String el) {
        if (el == null || el.isBlank()) {
            return el;
        }
        
        String trimmed = el.trim();
        
        // 如果已经是完整的 EL 表达式（以关键字开头），直接返回
        if (trimmed.startsWith("THEN(") || 
            trimmed.startsWith("WHEN(") || 
            trimmed.startsWith("IF(") || 
            trimmed.startsWith("SWITCH(") ||
            trimmed.startsWith("FOR(") ||
            trimmed.startsWith("WHILE(") ||
            trimmed.startsWith("ITERATOR(")) {
            return trimmed;
        }
        
        // 否则包装成 THEN(...)
        return "THEN(" + trimmed + ")";
    }
    
    /**
     * 从边数据中提取意图路由映射
     * 将 intent/intent_router 节点的出边转换为 sourceHandle → 目标节点ID(tag) 的映射
     * 存入上下文变量: __intent_routes_{nodeId}
     * 
     * 节点配置 Intents 中的 id 作为 sourceHandle，LLM 识别出意图后，
     * 通过 id 找到对应的目标节点 tag 进行路由
     * 
     * LiteFlow SWITCH 返回目标节点的 tag 值，LiteFlow 会自动匹配 TO 中的节点
     */
    private void extractIntentRoutesFromEdges(String nodesJson, String edgesJson, WorkflowContext context) {
        try {
            // 解析节点，找出所有 intent 或 intent_router 类型的节点
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            
            Set<String> intentNodeIds = nodes.stream()
                    .filter(n -> "intent".equals(n.type()) || "intent_router".equals(n.type()))
                    .map(WorkflowNodeDto::id)
                    .collect(java.util.stream.Collectors.toSet());
            
            if (intentNodeIds.isEmpty()) {
                return;
            }
            
            // 解析边
            List<WorkflowEdgeDto> edges = objectMapper.readValue(
                    edgesJson, new TypeReference<List<WorkflowEdgeDto>>() {});
            
            // 为每个意图节点构建路由映射
            // sourceHandle (即 intent 的 id) → 目标节点 ID（作为 tag 值）
            for (String intentNodeId : intentNodeIds) {
                Map<String, String> routeMap = new HashMap<>();
                
                for (WorkflowEdgeDto edge : edges) {
                    if (intentNodeId.equals(edge.source())) {
                        // sourceHandle 就是 intent 配置中的 id
                        String sourceHandle = edge.sourceHandle();
                        if (sourceHandle == null || sourceHandle.isEmpty()) {
                            // 如果没有 sourceHandle，使用 label 或 default
                            sourceHandle = edge.label();
                            if (sourceHandle == null || sourceHandle.isEmpty()) {
                                sourceHandle = "default";
                            }
                        }
                        
                        // 直接存储目标节点 ID（LiteFlow 会通过 tag 匹配）
                        routeMap.put(sourceHandle, edge.target());
                    }
                }
                
                if (!routeMap.isEmpty()) {
                    String routesKey = "__intent_routes_" + intentNodeId;
                    context.setVariable(routesKey, routeMap);
                    log.debug("提取意图路由映射: nodeId={}, routes={}", intentNodeId, routeMap);
                }
            }
            
        } catch (Exception e) {
            log.warn("提取意图路由映射失败", e);
        }
    }

    /**
     * 从边数据中提取工具节点的路由映射
     * 为每个 tool 节点构建路由映射：sourceHandle (如 "executed", "not_executed") → 目标节点 ID
     */
    private void extractToolRoutesFromEdges(String nodesJson, String edgesJson, WorkflowContext context) {
        try {
            // 解析节点，找出所有 tool 类型的节点
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            
            Set<String> toolNodeIds = nodes.stream()
                    .filter(n -> "tool".equals(n.type()))
                    .map(WorkflowNodeDto::id)
                    .collect(java.util.stream.Collectors.toSet());
            
            if (toolNodeIds.isEmpty()) {
                return;
            }
            
            // 解析边
            List<WorkflowEdgeDto> edges = objectMapper.readValue(
                    edgesJson, new TypeReference<List<WorkflowEdgeDto>>() {});
            
            // 为每个工具节点构建路由映射
            // sourceHandle (如 "executed", "not_executed") → 目标节点 ID
            for (String toolNodeId : toolNodeIds) {
                Map<String, String> routeMap = new HashMap<>();
                
                for (WorkflowEdgeDto edge : edges) {
                    if (toolNodeId.equals(edge.source())) {
                        // sourceHandle 就是工具节点返回的状态值（如 "executed", "not_executed"）
                        String sourceHandle = edge.sourceHandle();
                        if (sourceHandle == null || sourceHandle.isEmpty()) {
                            // 如果没有 sourceHandle，使用默认值
                            sourceHandle = "executed";
                        }
                        
                        // 存储目标节点 ID
                        routeMap.put(sourceHandle, edge.target());
                    }
                }
                
                if (!routeMap.isEmpty()) {
                    String routesKey = "__tool_routes_" + toolNodeId;
                    context.setVariable(routesKey, routeMap);
                    log.debug("提取工具节点路由映射: nodeId={}, routes={}", toolNodeId, routeMap);
                }
            }
            
        } catch (Exception e) {
            log.warn("提取工具节点路由映射失败", e);
        }
    }

    /**
     * 从边数据中提取条件节点的路由映射
     * 为每个 condition 节点构建路由映射：sourceHandle (条件ID) → 目标节点 ID
     */
    private void extractConditionRoutesFromEdges(String nodesJson, String edgesJson, WorkflowContext context) {
        try {
            // 解析节点，找出所有 condition 类型的节点
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            
            Set<String> conditionNodeIds = nodes.stream()
                    .filter(n -> "condition".equals(n.type()))
                    .map(WorkflowNodeDto::id)
                    .collect(java.util.stream.Collectors.toSet());
            
            if (conditionNodeIds.isEmpty()) {
                return;
            }
            
            // 解析边
            List<WorkflowEdgeDto> edges = objectMapper.readValue(
                    edgesJson, new TypeReference<List<WorkflowEdgeDto>>() {});
            
            // 为每个条件节点构建路由映射
            // sourceHandle (条件ID) → 目标节点 ID
            for (String conditionNodeId : conditionNodeIds) {
                Map<String, String> routeMap = new HashMap<>();
                
                for (WorkflowEdgeDto edge : edges) {
                    if (conditionNodeId.equals(edge.source())) {
                        String sourceHandle = edge.sourceHandle();
                        if (sourceHandle == null || sourceHandle.isEmpty()) {
                            // 如果没有 sourceHandle，可能是 else 分支或默认连接
                            sourceHandle = "else";
                        }
                        
                        // 存储目标节点 ID
                        routeMap.put(sourceHandle, edge.target());
                    }
                }
                
                if (!routeMap.isEmpty()) {
                    String routesKey = "__condition_routes_" + conditionNodeId;
                    context.setVariable(routesKey, routeMap);
                    log.debug("提取条件节点路由映射: nodeId={}, routes={}", conditionNodeId, routeMap);
                }
            }
            
        } catch (Exception e) {
            log.warn("提取条件节点路由映射失败", e);
        }
    }

    /**
     * 解析节点配置
     */
    private Map<String, JsonNode> parseNodesConfig(String nodesJson) {
        Map<String, JsonNode> result = new HashMap<>();
        try {
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            
            for (WorkflowNodeDto node : nodes) {
                if (node.data() != null && node.data().config() != null) {
                    result.put(node.id(), node.data().config());
                }
            }
        } catch (Exception e) {
            log.error("解析节点配置失败", e);
        }
        return result;
    }

    /**
     * 解析节点标签映射
     * key: nodeId, value: 节点标签（来自 data.label）
     */
    private Map<String, String> parseNodeLabels(String nodesJson) {
        Map<String, String> result = new HashMap<>();
        try {
            if (nodesJson == null || nodesJson.isEmpty()) {
                log.warn("节点JSON为空，无法解析标签");
                return result;
            }
            
            List<WorkflowNodeDto> nodes = objectMapper.readValue(
                    nodesJson, new TypeReference<List<WorkflowNodeDto>>() {});
            
            for (WorkflowNodeDto node : nodes) {
                if (node.data() != null && node.data().label() != null) {
                    result.put(node.id(), node.data().label());
                    log.debug("解析节点标签: nodeId={}, label={}", node.id(), node.data().label());
                } else {
                    log.debug("节点没有标签: nodeId={}", node.id());
                }
            }
            
            log.debug("解析节点标签完成: 总数={}, 有标签的节点数={}", nodes.size(), result.size());
        } catch (Exception e) {
            log.error("解析节点标签失败", e);
        }
        return result;
    }

    /**
     * 查找匹配的工作流
     */
    private AiWorkflow findMatchingWorkflow(ChatSession session) {
        // 1. 按分类匹配
        if (session.getCategory() != null) {
            String categoryId = session.getCategory().getId().toString();
            List<AiWorkflow> categoryWorkflows = workflowRepository.findByCategoryId(categoryId);
            if (!categoryWorkflows.isEmpty()) {
                return categoryWorkflows.get(0);
            }
        }

        // 2. 查找默认工作流
        return workflowRepository.findByIsDefaultTrueAndEnabledTrue().orElse(null);
    }

    /**
     * 工作流执行结果
     */
    public record WorkflowExecutionResult(
            boolean success,
            String reply,
            String errorMessage,
            String nodeDetailsJson,
            Boolean needHumanTransfer
    ) {}
}

