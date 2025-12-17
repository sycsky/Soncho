package com.example.aikef.workflow.service;

import com.example.aikef.dto.WorkflowTestSessionDto;
import com.example.aikef.dto.WorkflowTestSessionDto.*;
import com.example.aikef.model.AiWorkflow;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.Message;
import com.example.aikef.model.SessionCategory;
import com.example.aikef.model.WorkflowCategoryBinding;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.AiWorkflowRepository;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.repository.SessionCategoryRepository;
import com.example.aikef.repository.WorkflowCategoryBindingRepository;
import com.example.aikef.service.SessionMessageGateway;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流测试服务
 * 管理内存中的测试会话，支持多轮对话测试
 */
@Service
public class WorkflowTestService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowTestService.class);
    
    // 测试会话过期时间（30分钟）
    private static final long SESSION_EXPIRE_MINUTES = 30;
    
    // 内存中的测试会话
    private final Map<String, TestSession> testSessions = new ConcurrentHashMap<>();
    
    @Resource
    private AiWorkflowRepository workflowRepository;
    
    @Resource
    private AiWorkflowService workflowService;
    
    @Resource
    private ChatSessionRepository chatSessionRepository;
    
    @Resource
    private CustomerRepository customerRepository;
    
    @Resource
    private MessageRepository messageRepository;
    
    @Resource
    private SessionMessageGateway messageGateway;
    
    @Resource
    private SessionCategoryRepository categoryRepository;
    
    @Resource
    private WorkflowCategoryBindingRepository categoryBindingRepository;
    
    @Resource
    private AiWorkflowService aiWorkflowService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 内部测试会话类
     */
    private static class TestSession {
        String id;
        UUID workflowId;
        String workflowName;
        UUID sessionId;  // 测试用的ChatSession ID
        List<TestMessage> messages = new ArrayList<>();
        Map<String, Object> variables = new HashMap<>();
        Instant createdAt;
        Instant lastActiveAt;
        
        TestSession(String id, UUID workflowId, String workflowName, UUID sessionId) {
            this.id = id;
            this.workflowId = workflowId;
            this.workflowName = workflowName;
            this.sessionId = sessionId;
            this.createdAt = Instant.now();
            this.lastActiveAt = Instant.now();
        }
        
        void touch() {
            this.lastActiveAt = Instant.now();
        }
    }
    
    /**
     * 创建测试会话
     */
    @Transactional
    public WorkflowTestSessionDto createTestSession(UUID workflowId, Map<String, Object> initialVariables) {
        AiWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("工作流不存在: " + workflowId));
        
        String testSessionId = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        
        // 创建测试用的ChatSession（绑定到指定工作流）
        UUID sessionId = createTestChatSession(workflowId);
        
        TestSession session = new TestSession(testSessionId, workflowId, workflow.getName(), sessionId);
        if (initialVariables != null) {
            session.variables.putAll(initialVariables);
        }
        // 将sessionId添加到variables中，方便工作流使用
        session.variables.put("sessionId", sessionId.toString());
        
        testSessions.put(testSessionId, session);
        
        log.info("创建测试会话: testSessionId={}, sessionId={}, workflowId={}, workflowName={}", 
                testSessionId, sessionId, workflowId, workflow.getName());
        
        return toDto(session);
    }
    
    /**
     * 创建测试用的ChatSession（供API使用）
     */
    @Transactional
    public UUID createTestChatSessionForApi() {
        // API调用时没有指定工作流，创建一个不带分类的测试会话
        return createTestChatSession(null);
    }
    
    /**
     * 创建测试用的ChatSession
     * @param workflowId 要测试的工作流ID，如果提供则创建分类并绑定工作流
     */
    @Transactional
    private UUID createTestChatSession(UUID workflowId) {
        // 创建测试客户（每次测试创建新的，避免冲突）
        Customer testCustomer = new Customer();
        testCustomer.setName("测试用户_" + UUID.randomUUID().toString().substring(0, 8));
        testCustomer.setPrimaryChannel(com.example.aikef.model.Channel.WEB);
        testCustomer = customerRepository.save(testCustomer);
        
        // 创建测试会话
        ChatSession session = new ChatSession();
        session.setCustomer(testCustomer);
        session.setStatus(SessionStatus.AI_HANDLING);
        session.setCreatedAt(Instant.now());
        session.setLastActiveAt(Instant.now());
        
        // 如果指定了工作流，使用工作流已绑定的分类
        if (workflowId != null) {
            List<SessionCategory> categories = aiWorkflowService.getWorkflowCategories(workflowId);
            if (!categories.isEmpty()) {
                // 使用工作流绑定的第一个分类
                session.setCategory(categories.get(0));
                log.info("测试会话使用工作流绑定的分类: workflowId={}, categoryId={}, categoryName={}", 
                        workflowId, categories.get(0).getId(), categories.get(0).getName());
            } else {
                log.warn("工作流未绑定分类，测试会话将使用默认工作流: workflowId={}", workflowId);
            }
        }
        
        // 设置元数据，标记为测试会话
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "workflow_test");
            metadata.put("isTest", true);
            if (workflowId != null) {
                metadata.put("workflowId", workflowId.toString());
            }
            session.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.warn("设置测试会话metadata失败", e);
        }
        
        ChatSession saved = chatSessionRepository.save(session);
        return saved.getId();
    }
    
    
    /**
     * 发送测试消息
     */
    @Transactional
    public WorkflowTestSessionDto sendTestMessage(String testSessionId, String userMessage) {
        TestSession session = testSessions.get(testSessionId);
        if (session == null) {
            throw new IllegalArgumentException("测试会话不存在或已过期: " + testSessionId);
        }
        
        session.touch();
        
        // 保存用户消息到数据库
        Message userMessageEntity = messageGateway.sendAsCustomer(session.sessionId, userMessage);
        
        // 添加到测试会话的消息列表（用于前端显示）
        TestMessage userMsg = new TestMessage(
                userMessageEntity.getId().toString(),
                "user",
                userMessage,
                userMessageEntity.getCreatedAt(),
                null
        );
        session.messages.add(userMsg);
        
        // 执行工作流（使用executeForSession，更接近真实环境）
        long startTime = System.currentTimeMillis();
        AiWorkflowService.WorkflowExecutionResult result;
        
        try {
            // 使用executeForSession执行工作流，会自动匹配分类绑定的工作流
            // 测试会话可能没有 messageId，传递 null
            result = workflowService.executeForSession(session.sessionId, userMessage, userMessageEntity.getId());
        } catch (Exception e) {
            log.error("工作流执行异常: testSessionId={}", testSessionId, e);
            result = new AiWorkflowService.WorkflowExecutionResult(
                    false, null, e.getMessage(), null, false);
        }
        
        long durationMs = System.currentTimeMillis() - startTime;
        
        // 解析节点详情
        List<NodeDetail> nodeDetails = parseNodeDetails(result.nodeDetailsJson());
        
        // 创建消息元数据
        TestMessageMeta meta = new TestMessageMeta(
                result.success(),
                durationMs,
                result.errorMessage(),
                result.needHumanTransfer(),
                nodeDetails
        );
        
        // 添加助手回复
        String replyContent = result.reply();
        if (replyContent == null || replyContent.isEmpty()) {
            if (result.errorMessage() != null) {
                replyContent = "❌ 执行失败: " + result.errorMessage();
            } else {
                replyContent = "⚠️ 工作流未返回响应";
            }
        }
        
        // 保存AI回复消息到数据库
        Message aiMessageEntity = messageGateway.sendAiMessage(session.sessionId, replyContent);
        
        // 添加到测试会话的消息列表
        TestMessage assistantMsg = new TestMessage(
                aiMessageEntity.getId().toString(),
                "assistant",
                replyContent,
                aiMessageEntity.getCreatedAt(),
                meta
        );
        session.messages.add(assistantMsg);
        
        log.info("测试消息发送完成: testSessionId={}, success={}, durationMs={}, userMessageId={}, aiMessageId={}", 
                testSessionId, result.success(), durationMs, userMessageEntity.getId(), aiMessageEntity.getId());
        
        return toDto(session);
    }
    
    /**
     * 获取测试会话
     */
    public WorkflowTestSessionDto getTestSession(String testSessionId) {
        TestSession session = testSessions.get(testSessionId);
        if (session == null) {
            throw new IllegalArgumentException("测试会话不存在或已过期: " + testSessionId);
        }
        return toDto(session);
    }
    
    /**
     * 清除测试会话的对话历史
     */
    public WorkflowTestSessionDto clearTestSession(String testSessionId) {
        TestSession session = testSessions.get(testSessionId);
        if (session == null) {
            throw new IllegalArgumentException("测试会话不存在或已过期: " + testSessionId);
        }
        
        session.messages.clear();
        session.variables.clear();
        session.touch();
        
        log.info("清除测试会话: testSessionId={}", testSessionId);
        
        return toDto(session);
    }
    
    /**
     * 删除测试会话
     * 同时删除内存中的测试会话和数据库中的 ChatSession、Message
     */
    @Transactional
    public void deleteTestSession(String testSessionId) {
        TestSession removed = testSessions.remove(testSessionId);
        if (removed != null) {
            UUID sessionId = removed.sessionId;
            
            try {
                // 检查是否是测试会话
                ChatSession chatSession = chatSessionRepository.findById(sessionId).orElse(null);
                if (chatSession != null && isTestSession(chatSession)) {
                    // 先删除消息
                    List<Message> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
                    if (!messages.isEmpty()) {
                        messageRepository.deleteAll(messages);
                        log.debug("删除测试会话消息: sessionId={}, messageCount={}", sessionId, messages.size());
                    }
                    
                    // 删除 ChatSession
                    chatSessionRepository.deleteById(sessionId);
                    log.info("删除测试会话: testSessionId={}, sessionId={}, messageCount={}", 
                            testSessionId, sessionId, messages.size());
                } else {
                    log.info("删除测试会话（仅内存）: testSessionId={}, sessionId={}", testSessionId, sessionId);
                }
            } catch (Exception e) {
                log.warn("删除测试会话失败: testSessionId={}, sessionId={}", testSessionId, sessionId, e);
            }
        }
    }
    
    /**
     * 设置测试变量
     */
    public WorkflowTestSessionDto setTestVariables(String testSessionId, Map<String, Object> variables) {
        TestSession session = testSessions.get(testSessionId);
        if (session == null) {
            throw new IllegalArgumentException("测试会话不存在或已过期: " + testSessionId);
        }
        
        if (variables != null) {
            session.variables.putAll(variables);
        }
        session.touch();
        
        return toDto(session);
    }
    
    /**
     * 获取所有测试会话（用于管理）
     */
    public List<WorkflowTestSessionDto> getAllTestSessions() {
        return testSessions.values().stream()
                .map(this::toDto)
                .sorted((a, b) -> b.lastActiveAt().compareTo(a.lastActiveAt()))
                .toList();
    }
    
    /**
     * 定时清理过期的测试会话
     * 同时清理内存中的测试会话和数据库中的 ChatSession、Message
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void cleanupExpiredSessions() {
        Instant expireTime = Instant.now().minus(SESSION_EXPIRE_MINUTES, ChronoUnit.MINUTES);
        
        // 收集过期的测试会话
        List<TestSession> expiredSessions = testSessions.entrySet().stream()
                .filter(e -> e.getValue().lastActiveAt.isBefore(expireTime))
                .map(Map.Entry::getValue)
                .toList();
        
        if (expiredSessions.isEmpty()) {
            return;
        }
        
        // 收集所有过期的 sessionId
        List<UUID> expiredSessionIds = expiredSessions.stream()
                .map(s -> s.sessionId)
                .toList();
        
        // 删除数据库中的测试数据
        int deletedCount = 0;
        for (UUID sessionId : expiredSessionIds) {
            try {
                // 检查是否是测试会话（通过 metadata 判断）
                ChatSession chatSession = chatSessionRepository.findById(sessionId).orElse(null);
                if (chatSession != null && isTestSession(chatSession)) {
                    // 先删除消息（Message 有外键引用 ChatSession）
                    List<Message> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
                    if (!messages.isEmpty()) {
                        messageRepository.deleteAll(messages);
                        log.debug("删除测试会话消息: sessionId={}, messageCount={}", sessionId, messages.size());
                    }
                    
                    // 删除 ChatSession
                    chatSessionRepository.deleteById(sessionId);
                    deletedCount++;
                    log.debug("删除测试 ChatSession: sessionId={}", sessionId);
                }
            } catch (Exception e) {
                log.warn("清理测试会话失败: sessionId={}", sessionId, e);
            }
        }
        
        // 从内存中删除测试会话
        for (TestSession session : expiredSessions) {
            testSessions.remove(session.id);
        }
        
        if (!expiredSessions.isEmpty()) {
            log.info("清理了 {} 个过期测试会话（内存），删除了 {} 个测试 ChatSession（数据库）", 
                    expiredSessions.size(), deletedCount);
        }
    }
    
    /**
     * 判断是否是测试会话
     */
    private boolean isTestSession(ChatSession session) {
        if (session.getMetadata() == null || session.getMetadata().isBlank()) {
            return false;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(session.getMetadata(), Map.class);
            Boolean isTest = (Boolean) metadata.get("isTest");
            return Boolean.TRUE.equals(isTest);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 转换为 DTO
     */
    private WorkflowTestSessionDto toDto(TestSession session) {
        return new WorkflowTestSessionDto(
                session.id,
                session.workflowId,
                session.workflowName,
                new ArrayList<>(session.messages),
                session.createdAt,
                session.lastActiveAt
        );
    }
    
    /**
     * 解析节点详情
     */
    private List<NodeDetail> parseNodeDetails(String nodeDetailsJson) {
        if (nodeDetailsJson == null || nodeDetailsJson.isEmpty()) {
            return null;
        }
        
        try {
            List<WorkflowContext.NodeExecutionDetail> details = objectMapper.readValue(
                    nodeDetailsJson,
                    new TypeReference<List<WorkflowContext.NodeExecutionDetail>>() {}
            );
            
            return details.stream()
                    .map(d -> new NodeDetail(
                            d.getNodeId(),
                            d.getNodeType(),
                            d.getNodeLabel(),  // 节点标签
                            d.getInput() != null ? d.getInput().toString() : null,
                            d.getOutput() != null ? d.getOutput().toString() : null,
                            d.getDurationMs(),
                            d.isSuccess()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("解析节点详情失败", e);
            return null;
        }
    }
}

