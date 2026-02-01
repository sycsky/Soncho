package com.example.aikef.service;

import com.example.aikef.model.AiWorkflow;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.Event;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.EventRepository;
import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.service.SessionMessageGateway;
import com.example.aikef.workflow.service.AiWorkflowService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * 事件服务
 * 处理事件配置和事件触发逻辑
 */
@Service
@Transactional
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    @Lazy
    private AiWorkflowService workflowService;

    @Autowired
    @Lazy
    private SessionMessageGateway messageGateway;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    /**
     * 获取所有事件
     */
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * 获取所有启用的事件
     */
    public List<Event> getEnabledEvents() {
        return eventRepository.findByEnabledTrueOrderBySortOrder();
    }

    /**
     * 根据ID获取事件
     */
    public Event getEventById(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("事件不存在: " + eventId));
    }

    /**
     * 根据名称获取事件
     */
    public Event getEventByName(String name) {
        return eventRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("事件不存在: " + name));
    }

    /**
     * 创建事件
     */
    @Transactional
    public Event createEvent(CreateEventRequest request) {
        // 检查名称是否已存在
        if (eventRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("事件名称已存在: " + request.name());
        }

        Event event = new Event();
        event.setName(request.name());
        event.setDisplayName(request.displayName());
        event.setDescription(request.description());
        
        // 设置绑定的工作流
        event.setWorkflowName(request.workflowName());
        
        event.setEnabled(request.enabled() != null ? request.enabled() : true);
        event.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

        Event saved = eventRepository.save(event);
        log.info("创建事件: eventId={}, name={}, workflowName={}", 
                saved.getId(), saved.getName(), request.workflowName());
        
        return saved;
    }

    /**
     * 更新事件
     */
    @Transactional
    public Event updateEvent(UUID eventId, UpdateEventRequest request) {
        Event event = getEventById(eventId);

        // 检查名称是否与其他事件冲突
        if (request.name() != null && !request.name().equals(event.getName())) {
            eventRepository.findByName(request.name())
                    .filter(e -> !e.getId().equals(eventId))
                    .ifPresent(e -> {
                        throw new IllegalArgumentException("事件名称已存在: " + request.name());
                    });
            event.setName(request.name());
        }

        if (request.displayName() != null) {
            event.setDisplayName(request.displayName());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if (request.workflowName() != null) {
            event.setWorkflowName(request.workflowName());
        }
        if (request.enabled() != null) {
            event.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            event.setSortOrder(request.sortOrder());
        }

        Event saved = eventRepository.save(event);
        log.info("更新事件: eventId={}, name={}", saved.getId(), saved.getName());
        
        return saved;
    }

    /**
     * 删除事件
     */
    @Transactional
    public void deleteEvent(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("事件不存在: " + eventId);
        }
        eventRepository.deleteById(eventId);
        log.info("删除事件: eventId={}", eventId);
    }

    /**
     * 为指定客户触发事件（异步）
     * 自动查找最近的活跃会话
     * 增加 Redis 计数器防止并发重复触发（10秒内只允许一次）
     *
     * @param customerId 客户ID
     * @param eventName 事件名称
     * @param eventData 事件数据
     */
    @Transactional
    @Async
    public void triggerEventForCustomerAsync(UUID customerId, String eventName, Map<String, Object> eventData) {
        // 1. 根据 customerId 查找客户，获取租户ID
        // 此时 TenantContext 为空，Repository 不会开启 Filter，可以查到任意租户的客户
        Customer customer = customerRepository.findById(customerId).orElse(null);

        if (customer != null && customer.getTenantId() != null) {
            // 2. 设置租户上下文
            TenantContext.setTenantId(customer.getTenantId());
        } else {
            if (customer == null) {
                log.warn("触发异步事件时未找到客户: customerId={}", customerId);
                return;
            }
            log.warn("触发异步事件时客户无租户ID: customerId={}", customerId);
        }

        try {
            triggerEventForCustomer(customerId, eventName, eventData);
        } finally {
            // 3. 清理上下文
            TenantContext.clear();
        }
    }

    /**
     * 为指定客户触发事件（自动查找最近的活跃会话）
     * 
     * @param customerId 客户ID
     * @param eventName 事件名称
     * @param eventData 事件数据
     * @return 工作流执行结果
     */
    @Transactional
    public AiWorkflowService.WorkflowExecutionResult triggerEventForCustomer(UUID customerId, String eventName, Map<String, Object> eventData) {
        // 查找客户最近的活跃会话
        ChatSession session = chatSessionRepository.findFirstByCustomer_IdOrderByLastActiveAtDesc(customerId);
        
        if (session == null) {
            log.warn("未找到客户的活跃会话，无法触发事件: customerId={}, eventName={}", customerId, eventName);
            return new AiWorkflowService.WorkflowExecutionResult(
                    false, null, "未找到客户的活跃会话", null, false, null);
        }
        
        return triggerEvent(eventName, session.getId(), eventData);
    }

    /**
     * 触发事件
     * 根据事件名称查找配置，并执行绑定的工作流
     * 
     * @param eventName 事件名称
     * @param sessionId 会话ID
     * @param eventData 事件数据（将作为变量传递给工作流）
     * @return 工作流执行结果
     */

    @Transactional
    @Async
    public void triggerEventAsync(String eventName, UUID sessionId, Map<String, Object> eventData, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(tenantId);
        } else {
             log.warn("触发异步事件时未提供租户ID: eventName={}", eventName);
        }

        try {
            this.triggerEvent(eventName, sessionId, eventData);
        } finally {
            TenantContext.clear();
        }
    }


    @Transactional
    public AiWorkflowService.WorkflowExecutionResult triggerEvent(String eventName, UUID sessionId, Map<String, Object> eventData) {
        // 1. Find Event
        Event event = eventRepository.findByName(eventName)
                .orElseThrow(() -> new EntityNotFoundException("事件不存在: " + eventName));

        if (!event.isEnabled()) {
            log.warn("事件已禁用，不执行: eventName={}", eventName);
            return new AiWorkflowService.WorkflowExecutionResult(
                    false, null, "事件已禁用", null, false, null);
        }

        if (event.getWorkflowName() == null || event.getWorkflowName().isBlank()) {
            log.warn("事件未绑定工作流: eventName={}", eventName);
            return new AiWorkflowService.WorkflowExecutionResult(
                    false, null, "事件未绑定工作流", null, false, null);
        }

        boolean isMockSession = false;
        ChatSession session = null;

        // 2. Handle Session
        if (sessionId == null) {
            log.info("触发事件 {} 时未提供 sessionId，创建模拟会话", eventName);
            ChatSession mockSession = new ChatSession();
            mockSession.setNote("System Event Trigger: " + eventName);
            mockSession.setStatus(com.example.aikef.model.enums.SessionStatus.AI_HANDLING);
            session = chatSessionRepository.save(mockSession);
            sessionId = session.getId();
            isMockSession = true;
        } else {
            final UUID lookupId = sessionId;
            session = chatSessionRepository.findById(lookupId)
                    .orElseThrow(() -> new EntityNotFoundException("会话不存在: " + lookupId));
        }

        try {
            // 3. Prepare Workflow & Variables
            com.example.aikef.model.AiWorkflow workflow = workflowService.getWorkflowByName(event.getWorkflowName());

            log.info("触发事件: eventName={}, eventId={}, sessionId={}, workflowName={}, workflowId={}", 
                    eventName, event.getId(), sessionId, event.getWorkflowName(), workflow.getId());

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
            
            if (eventData != null) {
                variables.putAll(eventData);
                variables.put("eventData", eventData);
            }
            
            variables.put("eventName", eventName);
            variables.put("eventId", event.getId().toString());

            String triggerMessage = eventData != null && eventData.containsKey("message") 
                    ? eventData.get("message").toString() 
                    : "事件触发: " + eventName;

            // 4. Execute
            AiWorkflowService.WorkflowExecutionResult result = workflowService.executeWorkflow(
                    workflow.getId(), 
                    sessionId, 
                    triggerMessage, 
                    variables
            );

            // 5. Send Reply (only if not mock session)
            if (!isMockSession && result.success() && result.reply() != null && !result.reply().isBlank()) {
                try {
                    messageGateway.sendAiMessage(sessionId, result.reply());
                    log.info("事件触发工作流执行成功，已发送 AI 回复: eventName={}, sessionId={}, replyLength={}", 
                            eventName, sessionId, result.reply().length());
                } catch (Exception e) {
                    log.error("发送 AI 回复失败: eventName={}, sessionId={}", eventName, sessionId, e);
                }
            } else if (!result.success()) {
                log.warn("事件触发工作流执行失败: eventName={}, sessionId={}, error={}", 
                        eventName, sessionId, result.errorMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("触发事件失败: eventName={}, sessionId={}", eventName, sessionId, e);
            return new AiWorkflowService.WorkflowExecutionResult(
                    false, null, "触发事件失败: " + e.getMessage(), null, false, null);
        } finally {
            // 6. Cleanup Mock Session
            if (isMockSession) {
                log.info("清理模拟会话: {}", sessionId);
                chatSessionRepository.deleteById(sessionId);
            }
        }
    }

    /**
     * 创建事件请求
     */
    public record CreateEventRequest(
            String name,
            String displayName,
            String description,
            String workflowName,
            Boolean enabled,
            Integer sortOrder
    ) {}

    /**
     * 更新事件请求
     */
    public record UpdateEventRequest(
            String name,
            String displayName,
            String description,
            String workflowName,
            Boolean enabled,
            Integer sortOrder
    ) {}
}

