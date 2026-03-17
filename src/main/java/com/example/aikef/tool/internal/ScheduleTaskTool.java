package com.example.aikef.tool.internal;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.mongo.CustomerCoreMemory;
import com.example.aikef.repository.mongo.CustomerCoreMemoryRepository;
import com.example.aikef.service.SqsDelayService;
import com.example.aikef.tool.annotation.AutoInjectTool;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@AutoInjectTool
@RequiredArgsConstructor
public class ScheduleTaskTool {

    private static final int MAX_SQS_DELAY_SECONDS = 900;

    private final SqsDelayService sqsDelayService;
    private final CustomerCoreMemoryRepository customerCoreMemoryRepository;
    private final LangChainChatService langChainChatService;
    private final ObjectMapper objectMapper;

    @Tool("Schedule a task for later execution")
    public String scheduleTask(
            @P("Description of the task to perform") String taskDescription,
            @P("Execution time in ISO-8601 format (e.g., 2023-10-01T10:00:00)") String executeAt,
            @ToolMemoryId WorkflowContext context) {

        if (context == null || context.getCustomerId() == null) {
            return "Error: Customer context is missing. Cannot schedule task.";
        }

        try {
            if (taskDescription == null || taskDescription.isBlank()) {
                return "Error: taskDescription is required.";
            }
            LocalDateTime executionTime = LocalDateTime.parse(executeAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            if (!executionTime.isAfter(now)) {
                return "Error: Execution time must be in the future.";
            }

            String customerId = context.getCustomerId().toString();
            String memorySummary = customerCoreMemoryRepository.findByCustomerId(customerId)
                    .map(CustomerCoreMemory::getSummary)
                    .orElse("");

            // 短时间任务（<10分钟）强制跳过中途提醒规划，避免过度打扰
            long totalSeconds = ChronoUnit.SECONDS.between(now, executionTime);
            ReminderPlan reminderPlan;
            if (totalSeconds < 600) {
                reminderPlan = new ReminderPlan(taskDescription, List.of());
                log.info("Task duration {}s < 600s, skipping intermediate reminders.", totalSeconds);
            } else {
                reminderPlan = planReminders(taskDescription, now, executionTime, memorySummary);
            }
            
            Set<String> dedupeSet = new LinkedHashSet<>();
            List<ReminderItem> reminderItems = new ArrayList<>();
            String mainContent = normalizeContent(reminderPlan.mainReminderContent(), taskDescription);
            addReminder(reminderItems, dedupeSet, executionTime, mainContent);

            for (ReminderItem item : reminderPlan.intermediateReminders()) {
                if (item.executeAt().isAfter(now) && item.executeAt().isBefore(executionTime)) {
                    addReminder(reminderItems, dedupeSet, item.executeAt(), normalizeContent(item.content(), taskDescription));
                }
            }
            reminderItems.sort(Comparator.comparing(ReminderItem::executeAt));

            int intermediateCount = 0;
            for (ReminderItem item : reminderItems) {
                if (item.executeAt().isBefore(executionTime)) {
                    intermediateCount++;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("customerId", customerId);
                payload.put("task", item.content());
                payload.put("workflowId", context.getWorkflowId() == null ? null : context.getWorkflowId().toString());
                payload.put("sessionId", context.getSessionId() == null ? null : context.getSessionId().toString());
                payload.put("executeAt", item.executeAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                payload.put("reminderType", item.executeAt().isBefore(executionTime) ? "INTERMEDIATE" : "MAIN");
                payload.put("originTaskDescription", taskDescription);

                long seconds = ChronoUnit.SECONDS.between(now, item.executeAt());
                int initialDelay = (int) Math.min(MAX_SQS_DELAY_SECONDS, Math.max(1, seconds));
                sqsDelayService.sendDelayMessage(objectMapper.writeValueAsString(payload), initialDelay);
            }

            return "Task scheduled successfully. main=1, intermediate=" + intermediateCount + ", executeAt=" + executeAt;

        } catch (Exception e) {
            log.error("Failed to schedule task", e);
            return "Error scheduling task: " + e.getMessage();
        }
    }

    private ReminderPlan planReminders(String taskDescription, LocalDateTime now, LocalDateTime mainExecuteAt, String memorySummary) {
        try {
            String prompt = """
                    你是提醒规划助手。基于用户偏好决定是否需要中途提醒，并生成提醒内容。
                    输出必须是 JSON 对象，不要输出其他文本。
                    JSON 结构:
                    {
                      "main_reminder_content": "string",
                      "intermediate_reminders": [
                        {"execute_at":"ISO-8601 LocalDateTime","content":"string"}
                      ]
                    }
                    约束:
                    1) 仅在确有必要时添加中途提醒，最多 3 条。
                    2) 中途提醒必须早于主提醒时间，且晚于当前时间。
                    3) 内容自然、人性化、简洁，适用于通用场景，不局限会议。
                    4) 重要：main_reminder_content 是「主提醒时间点」发送给用户的文案，必须是“现在是X点，请立即XXX”的当前时态，严禁使用“10分钟后”、“即将”等未来时态。
                    5) intermediate_reminders 的 content 是在该中途时间点发送的文案，可以使用“还有X分钟”的预告时态。
                    6) 如果「当前时间」距离「主提醒时间」较近（如少于 30 分钟），请谨慎甚至不添加中途提醒，避免过度打扰。
                    7) **CRITICAL**: 检查【用户长期记忆摘要】。如果用户明确表示偏好（如“不要中途提醒”、“禁止中途打扰”等），必须严格遵守，不要生成任何 intermediate_reminders (返回空数组)。
                    当前时间: %s
                    主提醒时间: %s
                    用户原始任务: %s
                    用户长期记忆摘要: %s
                    """.formatted(
                    now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    mainExecuteAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    taskDescription,
                    memorySummary == null ? "" : memorySummary
            );
            String response = langChainChatService.simpleChat(prompt, "");
            String json = extractJson(response);
            JsonNode root = objectMapper.readTree(json);
            String mainContent = root.path("main_reminder_content").asText(taskDescription);

            List<ReminderItem> items = new ArrayList<>();
            JsonNode intermediates = root.path("intermediate_reminders");
            if (intermediates.isArray()) {
                for (JsonNode node : intermediates) {
                    String executeAt = node.path("execute_at").asText(null);
                    String content = node.path("content").asText(null);
                    if (executeAt == null || content == null || content.isBlank()) {
                        continue;
                    }
                    LocalDateTime t = LocalDateTime.parse(executeAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    if (t.isAfter(now) && t.isBefore(mainExecuteAt)) {
                        items.add(new ReminderItem(t, content.trim()));
                    }
                }
            }
            items.sort(Comparator.comparing(ReminderItem::executeAt));
            if (items.size() > 3) {
                items = new ArrayList<>(items.subList(0, 3));
            }
            return new ReminderPlan(mainContent, items);
        } catch (Exception e) {
            log.warn("Failed to plan intermediate reminders, fallback to main reminder only", e);
            return new ReminderPlan(taskDescription, List.of());
        }
    }

    private void addReminder(List<ReminderItem> items, Set<String> dedupeSet, LocalDateTime executeAt, String content) {
        String key = executeAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + content;
        if (dedupeSet.add(key)) {
            items.add(new ReminderItem(executeAt, content));
        }
    }

    private String normalizeContent(String content, String fallback) {
        if (content == null || content.isBlank()) {
            return fallback;
        }
        return content.trim();
    }

    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Invalid JSON response from LLM");
        }
        return response.substring(start, end + 1);
    }

    private record ReminderPlan(String mainReminderContent, List<ReminderItem> intermediateReminders) {}

    private record ReminderItem(LocalDateTime executeAt, String content) {}
}
