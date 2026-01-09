package com.example.aikef.controller;

import com.example.aikef.model.Event;
import com.example.aikef.service.EventService;
import com.example.aikef.workflow.service.AiWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 事件管理控制器
 * 提供事件配置的CRUD接口和事件hook接收接口
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 获取所有事件
     */
    @GetMapping
    public List<EventDto> getAllEvents() {
        return eventService.getAllEvents().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 获取所有启用的事件
     */
    @GetMapping("/enabled")
    public List<EventDto> getEnabledEvents() {
        return eventService.getEnabledEvents().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 根据ID获取事件
     */
    @GetMapping("/{eventId}")
    public EventDto getEventById(@PathVariable UUID eventId) {
        Event event = eventService.getEventById(eventId);
        return toDto(event);
    }

    /**
     * 创建事件
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto createEvent(@Valid @RequestBody EventService.CreateEventRequest request) {
        Event event = eventService.createEvent(request);
        return toDto(event);
    }

    /**
     * 更新事件
     */
    @PutMapping("/{eventId}")
    public EventDto updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventService.UpdateEventRequest request) {
        Event event = eventService.updateEvent(eventId, request);
        return toDto(event);
    }

    /**
     * 删除事件
     */
    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
    }

    /**
     * 接收外部事件hook
     * 
     * @param event 事件名称
     * @param request hook请求体，包含sessionId和eventData
     * @return 工作流执行结果
     */
    @PostMapping("/hook/{event}")
    public ResponseEntity<EventHookResponse> receiveEventHook(
            @PathVariable @NotBlank String event,
            @Valid @RequestBody EventHookRequest request) {
        
        try {
            AiWorkflowService.WorkflowExecutionResult result = eventService.triggerEvent(
                    event,
                    request.sessionId(),
                    request.eventData()
            );

            EventHookResponse response = new EventHookResponse(
                    true,
                    "事件触发成功",
                    result.success(),
                    result.reply(),
                    result.errorMessage()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            EventHookResponse response = new EventHookResponse(
                    false,
                    "事件触发失败: " + e.getMessage(),
                    false,
                    null,
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 转换为DTO
     */
    private EventDto toDto(Event event) {
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDisplayName(),
                event.getDescription(),
                event.getWorkflow() != null ? event.getWorkflow().getId() : null,
                event.getWorkflow() != null ? event.getWorkflow().getName() : null,
                event.isEnabled(),
                event.getSortOrder(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    /**
     * 事件DTO
     */
    public record EventDto(
            UUID id,
            String name,
            String displayName,
            String description,
            UUID workflowId,
            String workflowName,
            boolean enabled,
            Integer sortOrder,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}

    /**
     * 事件Hook请求
     */
    public record EventHookRequest(
            @NotNull UUID sessionId,
            Map<String, Object> eventData
    ) {}

    /**
     * 事件Hook响应
     */
    public record EventHookResponse(
            boolean success,
            String message,
            boolean workflowSuccess,
            String workflowReply,
            String workflowError
    ) {}
}





