package com.example.aikef.dto.request;

import com.example.aikef.model.enums.TaskCustomerMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SaveScheduledTaskRequest(
    @NotBlank(message = "任务名称不能为空")
    String name,
    
    String description,
    
    @NotNull(message = "工作流ID不能为空")
    UUID workflowId,
    
    @NotNull(message = "调度配置不能为空")
    ScheduleConfig scheduleConfig,
    
    @NotNull(message = "客户模式不能为空")
    TaskCustomerMode customerMode,
    
    List<String> targetIds,
    
    String initialInput,
    
    Boolean enabled
) {
    public record ScheduleConfig(
        String type, // DAILY, WEEKLY, MONTHLY
        List<Integer> daysOfWeek, // 1-7 (Mon-Sun)
        List<Integer> daysOfMonth, // 1-31
        String time // HH:mm:ss
    ) {}
}
