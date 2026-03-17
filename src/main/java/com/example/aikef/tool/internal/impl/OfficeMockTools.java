package com.example.aikef.tool.internal.impl;

import com.example.aikef.tool.annotation.AutoInjectTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@AutoInjectTool
public class OfficeMockTools {

    private static final Logger log = LoggerFactory.getLogger(OfficeMockTools.class);
    private static final AtomicInteger TASK_SEQUENCE = new AtomicInteger(1000);
    private static final AtomicInteger APPROVAL_SEQUENCE = new AtomicInteger(5000);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, OfficeTask> TASK_STORE = new ConcurrentHashMap<>();
    private static final Map<String, ApprovalRecord> APPROVAL_STORE = new ConcurrentHashMap<>();

    @Tool("Create a mock office task and return a task id. Must use user-confirmed values; do not assume owner/deadline/priority.")
    public String createOfficeTask(
            @P(value = "Task title", required = true) String title,
            @P(value = "Task owner", required = true) String owner,
            @P(value = "Deadline in format yyyy-MM-dd HH:mm", required = true) String deadline,
            @P(value = "Priority: LOW, MEDIUM, HIGH", required = false) String priority
    ) {
        String taskId = "TASK-" + TASK_SEQUENCE.incrementAndGet();
        String resolvedPriority = priority == null || priority.isBlank() ? "MEDIUM" : priority.toUpperCase();
        OfficeTask task = new OfficeTask(taskId, title, owner, deadline, resolvedPriority, "TODO", LocalDateTime.now());
        TASK_STORE.put(taskId, task);
        log.info("Created mock office task: {}", taskId);
        return "Task created: " + taskId + ", owner=" + owner + ", deadline=" + deadline + ", priority=" + resolvedPriority;
    }

    @Tool("Update status of a mock office task")
    public String updateOfficeTaskStatus(
            @P(value = "Task id, example TASK-1001", required = true) String taskId,
            @P(value = "Status: TODO, IN_PROGRESS, DONE, BLOCKED", required = true) String status
    ) {
        OfficeTask task = TASK_STORE.get(taskId);
        if (task == null) {
            return "Error: task not found: " + taskId;
        }
        task.status = status == null ? task.status : status.toUpperCase();
        return "Task updated: " + taskId + ", status=" + task.status;
    }

    @Tool("Submit a mock approval request and return approval id. Must use user-confirmed title/approver/reason.")
    public String submitApprovalRequest(
            @P(value = "Request title", required = true) String title,
            @P(value = "Approver", required = true) String approver,
            @P(value = "Business reason", required = true) String reason
    ) {
        String approvalId = "APR-" + APPROVAL_SEQUENCE.incrementAndGet();
        ApprovalRecord record = new ApprovalRecord(approvalId, title, approver, reason, "PENDING", LocalDateTime.now());
        APPROVAL_STORE.put(approvalId, record);
        log.info("Created mock approval request: {}", approvalId);
        return "Approval submitted: " + approvalId + ", approver=" + approver + ", status=PENDING";
    }

    @Tool("Process a mock approval request")
    public String processApprovalRequest(
            @P(value = "Approval id, example APR-5001", required = true) String approvalId,
            @P(value = "Decision: APPROVED or REJECTED", required = true) String decision
    ) {
        ApprovalRecord record = APPROVAL_STORE.get(approvalId);
        if (record == null) {
            return "Error: approval not found: " + approvalId;
        }
        String resolvedDecision = decision == null ? "" : decision.toUpperCase();
        if (!"APPROVED".equals(resolvedDecision) && !"REJECTED".equals(resolvedDecision)) {
            return "Error: decision must be APPROVED or REJECTED";
        }
        record.status = resolvedDecision;
        return "Approval processed: " + approvalId + ", status=" + record.status;
    }

    @Tool("Schedule a mock office meeting. Must use user-confirmed topic/date/time/participants.")
    public String scheduleOfficeMeeting(
            @P(value = "Meeting topic", required = true) String topic,
            @P(value = "Meeting date in format yyyy-MM-dd", required = true) String date,
            @P(value = "Meeting time in format HH:mm", required = true) String time,
            @P(value = "Participants separated by comma", required = true) String participants
    ) {
        LocalDate meetingDate = LocalDate.parse(date);
        LocalTime meetingTime = LocalTime.parse(time);
        String meetingId = "MEET-" + TASK_SEQUENCE.incrementAndGet();
        return "Meeting scheduled: " + meetingId + ", topic=" + topic + ", at=" + LocalDateTime.of(meetingDate, meetingTime).format(DATE_TIME_FORMATTER) + ", participants=" + participants;
    }

    @Tool("List latest mock office records for quick checking")
    public String listOfficeMockRecords(
            @P(value = "Record type: TASK or APPROVAL", required = true) String type
    ) {
        if ("TASK".equalsIgnoreCase(type)) {
            List<OfficeTask> items = new ArrayList<>(TASK_STORE.values());
            items.sort(Comparator.comparing(o -> o.createdAt, Comparator.reverseOrder()));
            return formatTaskRecords(items);
        }
        if ("APPROVAL".equalsIgnoreCase(type)) {
            List<ApprovalRecord> items = new ArrayList<>(APPROVAL_STORE.values());
            items.sort(Comparator.comparing(o -> o.createdAt, Comparator.reverseOrder()));
            return formatApprovalRecords(items);
        }
        return "Error: type must be TASK or APPROVAL";
    }

    private String formatTaskRecords(List<OfficeTask> tasks) {
        if (tasks.isEmpty()) {
            return "No task records";
        }
        StringBuilder builder = new StringBuilder("Task records:\n");
        int max = Math.min(tasks.size(), 10);
        for (int i = 0; i < max; i++) {
            OfficeTask task = tasks.get(i);
            builder.append(task.id)
                    .append(" | ")
                    .append(task.title)
                    .append(" | owner=").append(task.owner)
                    .append(" | deadline=").append(task.deadline)
                    .append(" | priority=").append(task.priority)
                    .append(" | status=").append(task.status)
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String formatApprovalRecords(List<ApprovalRecord> approvals) {
        if (approvals.isEmpty()) {
            return "No approval records";
        }
        StringBuilder builder = new StringBuilder("Approval records:\n");
        int max = Math.min(approvals.size(), 10);
        for (int i = 0; i < max; i++) {
            ApprovalRecord record = approvals.get(i);
            builder.append(record.id)
                    .append(" | ")
                    .append(record.title)
                    .append(" | approver=").append(record.approver)
                    .append(" | status=").append(record.status)
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private static class OfficeTask {
        private final String id;
        private final String title;
        private final String owner;
        private final String deadline;
        private final String priority;
        private String status;
        private final LocalDateTime createdAt;

        private OfficeTask(String id, String title, String owner, String deadline, String priority, String status, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.owner = owner;
            this.deadline = deadline;
            this.priority = priority;
            this.status = status;
            this.createdAt = createdAt;
        }
    }

    private static class ApprovalRecord {
        private final String id;
        private final String title;
        private final String approver;
        private final String reason;
        private String status;
        private final LocalDateTime createdAt;

        private ApprovalRecord(String id, String title, String approver, String reason, String status, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.approver = approver;
            this.reason = reason;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
}
