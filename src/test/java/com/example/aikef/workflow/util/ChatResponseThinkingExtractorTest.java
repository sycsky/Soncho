package com.example.aikef.workflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatResponseThinkingExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExtractThinkingFromTextAndCleanText() {
        String rawText = "<think>This is the thinking process.</think>\nHere is the final answer.";
        AiMessage aiMessage = AiMessage.from(rawText);
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(aiMessage);

        AiMessage enriched = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);

        assertNotNull(enriched);
        assertEquals("This is the thinking process.", enriched.thinking());
        assertEquals("Here is the final answer.", enriched.text());
    }

    @Test
    void shouldHandleMultiLineThinking() {
        String rawText = "<think>\nStep 1: Analyze.\nStep 2: Solve.\n</think>Answer.";
        AiMessage aiMessage = AiMessage.from(rawText);
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(aiMessage);

        AiMessage enriched = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);

        assertNotNull(enriched);
        // thinking() might trim the result, let's verify exact expectation
        // My code: thinking = extractedThinking.trim();
        // Extracted: "\nStep 1: Analyze.\nStep 2: Solve.\n" -> trim -> "Step 1: Analyze.\nStep 2: Solve."
        assertEquals("Step 1: Analyze.\nStep 2: Solve.", enriched.thinking());
        assertEquals("Answer.", enriched.text());
    }

    @Test
    void shouldNotChangeIfNoThinkingTags() {
        String rawText = "Just an answer.";
        AiMessage aiMessage = AiMessage.from(rawText);
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(aiMessage);

        AiMessage enriched = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);

        // Should return same object if no change
        assertSame(aiMessage, enriched);
    }
    
    @Test
    void shouldCaseInsensitiveTags() {
        String rawText = "<THINK>Thinking...</THINK>Answer";
        AiMessage aiMessage = AiMessage.from(rawText);
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(aiMessage);

        AiMessage enriched = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);

        assertEquals("Thinking...", enriched.thinking());
        assertEquals("Answer", enriched.text());
    }
}
