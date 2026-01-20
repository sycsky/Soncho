package com.example.aikef.service;

import com.example.aikef.model.AiWorkflow;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Event;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.EventRepository;
import com.example.aikef.service.SessionMessageGateway;
import com.example.aikef.workflow.service.AiWorkflowService;
import jakarta.annotation.Resource;
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

/**
 * 事件服务
 * 处理事件配置和事件触发逻辑
 */
@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    @Resource
    private EventRepository eventRepository;

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private AiWorkflowService workflowService;

    @Resource
    private SessionMessageGateway messageGateway;

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
        AiWorkflow workflow = workflowService.getWorkflow(request.workflowId());
        event.setWorkflow(workflow);
        
        event.setEnabled(request.enabled() != null ? request.enabled() : true);
        event.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

        Event saved = eventRepository.save(event);
        log.info("创建事件: eventId={}, name={}, workflowId={}", 
                saved.getId(), saved.getName(), workflow.getId());
        
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
        if (request.workflowId() != null) {
            AiWorkflow workflow = workflowService.getWorkflow(request.workflowId());
            event.setWorkflow(workflow);
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
    public void triggerEventAsync(String eventName, UUID sessionId, Map<String, Object> eventData) {
        this.triggerEvent(eventName, sessionId, eventData);
    }


    @Transactional
    public AiWorkflowService.WorkflowExecutionResult triggerEvent(String eventName, UUID sessionId, Map<String, Object> eventData) {
        // 查找事件配置
        Event event = eventRepository.findByName(eventName)
                .orElseThrow(() -> new EntityNotFoundException("事件不存在: " + eventName));

        if (!event.isEnabled()) {
            log.warn("事件已禁用，不执行: eventName={}", eventName);
            return new AiWorkflowService.WorkflowExecutionResult(
                    false, null, "事件已禁用: " + eventName, null, false, null);
        }

        // 验证会话是否存在
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在: " + sessionId));

        log.info("触发事件: eventName={}, eventId={}, sessionId={}, workflowId={}", 
                eventName, event.getId(), sessionId, event.getWorkflow().getId());

        // 构建工作流变量
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
        
        // 添加事件数据到变量中
        if (eventData != null) {
            variables.putAll(eventData);
            // 同时将整个eventData作为一个对象传递
            variables.put("eventData", eventData);
        }
        
        // 添加事件信息到变量
        variables.put("eventName", eventName);
        variables.put("eventId", event.getId().toString());

        // 执行绑定的工作流
        // 注意：事件触发时没有用户消息，使用空字符串或事件名称作为触发消息
        String triggerMessage = eventData != null && eventData.containsKey("message") 
                ? eventData.get("message").toString() 
                : "事件触发: " + eventName;

        AiWorkflowService.WorkflowExecutionResult result = workflowService.executeWorkflow(
                event.getWorkflow().getId(), 
                sessionId, 
                triggerMessage, 
                variables
        );

        // 如果工作流执行成功且有回复，发送 AI 回复消息（类似用户对话工作流）
        if (result.success() && result.reply() != null && !result.reply().isBlank()) {
            try {
                messageGateway.sendAiMessage(sessionId, result.reply());
                log.info("事件触发工作流执行成功，已发送 AI 回复: eventName={}, sessionId={}, replyLength={}", 
                        eventName, sessionId, result.reply().length());
            } catch (Exception e) {
                log.error("发送 AI 回复失败: eventName={}, sessionId={}", eventName, sessionId, e);
                // 即使发送失败，也返回工作流执行结果
            }
        } else if (!result.success()) {
            log.warn("事件触发工作流执行失败: eventName={}, sessionId={}, error={}", 
                    eventName, sessionId, result.errorMessage());
        }

        return result;
    }

    /**
     * 创建事件请求
     */
    public record CreateEventRequest(
            String name,
            String displayName,
            String description,
            UUID workflowId,
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
            UUID workflowId,
            Boolean enabled,
            Integer sortOrder
    ) {}
}

