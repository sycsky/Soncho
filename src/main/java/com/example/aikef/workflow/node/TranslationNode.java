package com.example.aikef.workflow.node;

import cn.hutool.json.JSONObject;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.exception.WorkflowPausedException;
import com.example.aikef.workflow.util.HistoryMessageLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 翻译节点
 * 根据历史对话判断用户的目标语言，然后翻译目标文本
 *
 * 配置示例:
 * {
 *   "modelId": "uuid",
 *   "prompt": "风格要求：正式",    // 附加提示词
 *   "targetText": "{{var.text}}", // 目标文本
 *   "historyCount": 10,           // 获取历史对话条数
 *   "outputVar": "translationResult" // 输出变量名
 * }
 */
@LiteflowComponent("translation")
public class TranslationNode extends BaseWorkflowNode {

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private HistoryMessageLoader historyMessageLoader;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 1. 获取配置
            JsonNode config = getNodeConfig();
            String modelIdStr = getConfigString("modelId", null);
            String additionalPrompt = getConfigString("systemPrompt", "");
            String targetText = getConfigString("targetText", "");
            int historyCount = getConfigInt("historyCount", 10);
            String outputVar = getConfigString("outputVar", "translationResult");

            // 变量替换
            targetText = renderTemplate(targetText);
            additionalPrompt = renderTemplate(additionalPrompt);

            if (!StringUtils.hasText(targetText)) {
                log.warn("翻译节点: 目标文本为空，跳过翻译");
                return;
            }

            // 2. 加载历史对话
            List<String> historyLines = new ArrayList<>();
            if (ctx.getSessionId() != null && historyCount > 0) {
                List<Message> historyMessages = historyMessageLoader.loadHistoryMessages(
                        ctx.getSessionId(), historyCount, ctx.getMessageId());
                
                for (Message msg : historyMessages) {

                    if(msg.getSenderType() == SenderType.USER){
                        historyLines.add("User: " + msg.getText());
                    }

                }
            }
            String historyContext = String.join("\n", historyLines);

            // 3. 构建 Prompt
            List<ChatMessage> messages = new ArrayList<>();
            
            // System Prompt
            String systemPrompt = """
                You are an intelligent translation assistant. Your task is to:
                1. Analyze the provided conversation history to identify the language used by the User.
                2. Translate the 'Target Text' into the identified User's language.
                3. If the User's language cannot be determined, translate into English by default.
                4. Output ONLY the translated text. Do not include any explanations, notes, or original text.
                """;
            messages.add(SystemMessage.from(systemPrompt));

            // User Prompt
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Conversation History:\n");
            if (historyContext.isBlank()) {
                userPrompt.append("(No history available)\n");
            } else {
                userPrompt.append(historyContext).append("\n");
            }
            
            userPrompt.append("\nTarget Text:\n").append(targetText).append("\n");
            
            if (StringUtils.hasText(additionalPrompt)) {
                userPrompt.append("\nAdditional Instructions:\n").append(additionalPrompt).append("\n");
            }
            
            messages.add(UserMessage.from(userPrompt.toString()));

            // 4. 调用 LLM
            UUID modelId = parseModelId(modelIdStr);
            
            // 转换消息格式
            List<LangChainChatService.ChatHistoryMessage> chatHistoryMessages = new ArrayList<>();
            for (ChatMessage msg : messages) {
                if (msg instanceof SystemMessage) {
                    chatHistoryMessages.add(new LangChainChatService.ChatHistoryMessage("system", ((SystemMessage) msg).text()));
                } else if (msg instanceof UserMessage) {
                    chatHistoryMessages.add(new LangChainChatService.ChatHistoryMessage("user", ((UserMessage) msg).singleText()));
                }
            }

            LangChainChatService.LlmChatResponse response = langChainChatService.chatWithMessages(
                    modelId,
                    null, // System prompt is already in messages
                    chatHistoryMessages,
                    0.3, // Low temperature for deterministic translation
                    null
            );

            if (!response.success()) {
                throw new RuntimeException("LLM Translation Failed: " + response.errorMessage());
            }

            String translatedText = response.reply().trim();
            log.info("Translation result: {}", translatedText);

            // 5. 保存结果
            ctx.setOutput(this.getTag(), translatedText);
            
            // 记录执行
            String inputInfo = "Target: " + targetText + "\nPrompt: " + additionalPrompt;
            recordExecution(inputInfo, translatedText, startTime, true, null);

        } catch (WorkflowPausedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Translation node failed", e);
            recordExecution("Translation Failed", null, startTime, false, e.getMessage());
            // 根据业务需求，这里可以选择抛出异常或者忽略
            // throw new RuntimeException(e);
        }
    }

    private UUID parseModelId(String modelIdStr) {
        if (modelIdStr == null || modelIdStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(modelIdStr);
        } catch (Exception e) {
            log.warn("Invalid Model ID: {}", modelIdStr);
            return null;
        }
    }
}
