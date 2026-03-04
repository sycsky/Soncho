package com.example.aikef.tool.internal;

import com.example.aikef.service.SqsDelayService;
import com.example.aikef.workflow.context.WorkflowContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskTool {

    private final SqsDelayService sqsDelayService;

    @Tool("Schedule a task for later execution")
    public String scheduleTask(
            @P("Description of the task to perform") String taskDescription,
            @P("Execution time in ISO-8601 format (e.g., 2023-10-01T10:00:00)") String executeAt,
            @ToolMemoryId WorkflowContext context) {

        if (context == null || context.getCustomerId() == null) {
            return "Error: Customer context is missing. Cannot schedule task.";
        }

        try {
            LocalDateTime executionTime = LocalDateTime.parse(executeAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();

            long delaySeconds = ChronoUnit.SECONDS.between(now, executionTime);

            if (delaySeconds <= 0) {
                return "Error: Execution time must be in the future.";
            }

            // Construct payload
            // We need a way to execute this task later. 
            // For now, we just log it or maybe trigger a specific workflow?
            // The requirement says "remind me to do something" or "give me a report".
            // Ideally, this should trigger a "TaskExecutionWorkflow".
            
            String payload = String.format("{\"customerId\": \"%s\", \"task\": \"%s\", \"workflowId\": \"%s\", \"sessionId\": \"%s\"}", 
                    context.getCustomerId(), taskDescription, context.getWorkflowId(), context.getSessionId());

            // Using SqsDelayService to send message
            // Note: SqsDelayService might need to be adapted to handle generic tasks
            // or we send to a specific queue.
            // For this POC, we assume SqsDelayService handles it.
            sqsDelayService.sendDelayMessage(payload, (int) delaySeconds);

            return "Task scheduled successfully for " + executeAt;

        } catch (Exception e) {
            log.error("Failed to schedule task", e);
            return "Error scheduling task: " + e.getMessage();
        }
    }
}
