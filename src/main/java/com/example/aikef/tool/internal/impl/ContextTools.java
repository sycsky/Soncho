package com.example.aikef.tool.internal.impl;

import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextTools {

    private final ObjectMapper objectMapper;

    @Tool("Get current workflow context variables and state. This tool allows the AI to inspect the current state of the workflow execution.")
    public String getWorkflowContext(
            @P(value = "Optional list of keys to filter the output (e.g. ['variables', 'nodeOutputs', 'customerInfo','sessionId','workflowId','nowTime']). If empty, returns all main context variables.", required = false) String[] keys,
            @ToolMemoryId WorkflowContext ctx
    ) {
        try {
            if (ctx == null) {
                return "Error: WorkflowContext is not available (null).";
            }

            Map<String, Object> result = new HashMap<>();

            // Add standard context fields
            result.put("workflowId", ctx.getWorkflowId());
            result.put("sessionId", ctx.getSessionId());
            result.put("query", ctx.getQuery());
            result.put("intent", ctx.getIntent());
            result.put("entities", ctx.getEntities());
            result.put("variables", ctx.getVariables());
            result.put("nodeOutputs", ctx.getNodeOutputs());
            result.put("customerInfo", ctx.getCustomerInfo());
            result.put("sessionMetadata", ctx.getSessionMetadata());
            result.put("nowTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            result.put("files", ctx.getFiles());

            // Filter if keys provided
            if (keys != null && keys.length > 0) {
                Map<String, Object> filtered = new HashMap<>();
                for (String key : keys) {
                    if (result.containsKey(key)) {
                        filtered.put(key, result.get(key));
                    } else if (ctx.getVariables() != null && ctx.getVariables().containsKey(key)) {
                        // Allow direct access to specific custom variables
                        filtered.put(key, ctx.getVariables().get(key));
                    }
                }
                return objectMapper.writeValueAsString(filtered);
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error executing getWorkflowContext", e);
            return "Error executing getWorkflowContext: " + e.getMessage();
        }
    }
}
