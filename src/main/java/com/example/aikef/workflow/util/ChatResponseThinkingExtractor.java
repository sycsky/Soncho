package com.example.aikef.workflow.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatResponseThinkingExtractor {

    private ChatResponseThinkingExtractor() {
    }

    public static AiMessage enrichAiMessage(ChatResponse response, ObjectMapper objectMapper) {
        if (response == null) {
            return null;
        }

        AiMessage aiMessage = response.aiMessage();
        if (aiMessage == null) {
            return null;
        }

        String text = aiMessage.text();
        String thinking = aiMessage.thinking();
        boolean textModified = false;

        // 1. Try to extract from text
        if (StringUtils.hasText(text)) {
            Pattern pattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String extractedThinking = matcher.group(1);
                if (StringUtils.hasText(extractedThinking)) {
                    thinking = extractedThinking.trim();
                    text = matcher.replaceFirst("").trim();
                    textModified = true;
                }
            }
        }

        // 2. If no thinking yet, try metadata
        if (!StringUtils.hasText(thinking)) {
            thinking = extractThinkingFromMetadata(response, objectMapper);
        }

        boolean thinkingChanged = StringUtils.hasText(thinking) && !thinking.equals(aiMessage.thinking());

        if (!textModified && !thinkingChanged) {
            return aiMessage;
        }

        return AiMessage.builder()
                .text(text)
                .thinking(thinking)
                .toolExecutionRequests(aiMessage.toolExecutionRequests() != null ? aiMessage.toolExecutionRequests() : Collections.emptyList())
                .attributes(aiMessage.attributes())
                .build();
    }

    private static String extractThinkingFromMetadata(ChatResponse response, ObjectMapper objectMapper) {
        if (response == null || objectMapper == null) {
            return null;
        }

        Object metadata = response.metadata();
        String body = null;

        try {
            if (metadata instanceof OpenAiChatResponseMetadata openAiMetadata) {
                if (openAiMetadata.rawHttpResponse() != null) {
                    body = openAiMetadata.rawHttpResponse().body();
                }
            } else if (metadata != null) {
                Method rawHttpResponseMethod = metadata.getClass().getMethod("rawHttpResponse");
                Object rawHttpResponse = rawHttpResponseMethod.invoke(metadata);
                if (rawHttpResponse != null) {
                    Method bodyMethod = rawHttpResponse.getClass().getMethod("body");
                    Object bodyObj = bodyMethod.invoke(rawHttpResponse);
                    if (bodyObj != null) {
                        body = bodyObj.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (!StringUtils.hasText(body)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messageNode = root.path("choices").path(0).path("message");
            if (messageNode == null || !messageNode.isObject()) {
                return null;
            }

            String[] keys = new String[]{"reasoning", "thinking", "reasoning_content", "analysis"};
            for (String key : keys) {
                JsonNode candidate = messageNode.get(key);
                if (candidate != null && candidate.isTextual()) {
                    String value = candidate.asText();
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}

