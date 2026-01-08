package com.example.aikef.dto;

import com.example.aikef.model.enums.TaskCustomerMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScheduledTaskDto(
    UUID id,
    String name,
    String description,
    UUID workflowId,
    String workflowName,
    Object scheduleConfig,
    TaskCustomerMode customerMode,
    List<String> targetIds,
    String initialInput,
    Boolean enabled,
    String cronExpression,
    Instant lastRunAt,
    Instant nextRunAt,
    Instant createdAt,
    Instant updatedAt
) {}
