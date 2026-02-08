package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.util.TemplateEngine;
import com.example.aikef.llm.LangChainChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * YES/NO Switch Node
 * Uses an LLM to evaluate a prompt and returns "YES" or "NO"
 */
@LiteflowComponent("yes_no")
public class YesNoNode extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(YesNoNode.class);

    @Autowired
    private LangChainChatService langChainChatService;

    @Override
    public String processSwitch() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = BaseWorkflowNode.resolveActualNodeId(this.getTag(), this.getNodeId(), ctx);
        JsonNode config = ctx.getNodeConfig(actualNodeId);

        String modelIdStr = BaseWorkflowNode.readConfigString(config, "modelId", null);
        // Fallback to "model" if modelId is not present (backward compatibility)
        if (modelIdStr == null) {
            modelIdStr = BaseWorkflowNode.readConfigString(config, "model", null);
        }
        
        String promptTemplate = BaseWorkflowNode.readConfigString(config, "systemPrompt", ""); // Using systemPrompt field for the prompt text

        // Parse Model ID
        UUID modelId = BaseWorkflowNode.parseUuidValue(modelIdStr);
        if (modelId == null) {
             BaseWorkflowNode.recordExecution(ctx, actualNodeId, "yes_no", this.getName(), 
                promptTemplate, null, startTime, false, "Model ID is required");
             throw new IllegalArgumentException("Model ID is required for YesNoNode");
        }

        // Render Prompt
        String prompt = TemplateEngine.render(promptTemplate, ctx);
        
        // Prepare LLM Input
        // We wrap the user's prompt with a strict instruction to output only YES or NO
        String systemInstruction = "You are a boolean decision maker. You must strictly answer with only 'YES' or 'NO'. Do not provide any explanation or other text.";
        
        // Execute LLM
        String result = "NO"; // Default
        String errorMessage = null;
        boolean success = true;

        try {
            // We use chatWithModel but we need to construct the messages appropriately.
            // Since LangChainChatService might be complex, let's assume a simple usage or check how LlmNode does it.
            // chat(UUID modelId, String systemPrompt, String userMessage, List<ChatHistoryMessage> chatHistory, Double temperature, Integer maxTokens)
            LangChainChatService.LlmChatResponse response = langChainChatService.chat(
                modelId, 
                systemInstruction, 
                prompt, 
                new java.util.ArrayList<>(), 
                0.0, 
                100
            );
            
            if (response.success()) {
                result = response.reply();
            } else {
                success = false;
                errorMessage = response.errorMessage();
                result = "NO";
            }
            
            // Post-process result to ensure it's strictly YES or NO
            result = result.trim().toUpperCase();
            if (result.contains("YES")) {
                result = "YES";
            } else {
                result = "NO";
            }
            
        } catch (Exception e) {
            log.error("Error executing YesNoNode", e);
            errorMessage = e.getMessage();
            success = false;
            // In case of error, we might want to default to NO or throw exception? 
            // LiteFlow Switch cannot return null.
            result = "NO"; 
        }

        // Set output (optional, but good for debugging/chaining)
        ctx.setOutput(this.getTag(), result);

        // Record Execution
        BaseWorkflowNode.recordExecution(ctx, actualNodeId, "yes_no", this.getName(), 
                prompt, result, startTime, success, errorMessage);

        log.info("YesNoNode execution: nodeId={}, result={}", actualNodeId, result);

        // Return the tag (YES or NO) which matches the edge sourceHandle
        return "tag:" + result; 
    }
}
