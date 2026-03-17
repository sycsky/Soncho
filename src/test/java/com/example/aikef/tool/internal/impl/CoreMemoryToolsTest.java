package com.example.aikef.tool.internal.impl;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.mongo.CustomerCoreMemory;
import com.example.aikef.repository.mongo.CustomerCoreMemoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreMemoryToolsTest {

    @Mock
    private CustomerCoreMemoryRepository coreMemoryRepository;
    @Mock
    private LangChainChatService langChainChatService;

    private CoreMemoryTools coreMemoryTools;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        coreMemoryTools = new CoreMemoryTools(coreMemoryRepository, langChainChatService, new ObjectMapper());
    }

    @Test
    void manageLongTermMemory_ShouldReplaceConflictedName_WhenLlmUpdateFails() {
        CustomerCoreMemory existing = new CustomerCoreMemory();
        existing.setCustomerId("c1");
        existing.setMemories(List.of("助手的名字是Alice", "你喜欢叫我Jxm"));

        when(coreMemoryRepository.findByCustomerId("c1")).thenReturn(Optional.of(existing));
        when(langChainChatService.simpleChat(anyString(), anyString()))
                .thenThrow(new RuntimeException("llm failed"))
                .thenReturn("summary");
        when(coreMemoryRepository.save(org.mockito.ArgumentMatchers.any(CustomerCoreMemory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        coreMemoryTools.manageLongTermMemory("c1", "助手的名字是Tom", null);

        ArgumentCaptor<CustomerCoreMemory> memoryCaptor = ArgumentCaptor.forClass(CustomerCoreMemory.class);
        verify(coreMemoryRepository).save(memoryCaptor.capture());
        CustomerCoreMemory saved = memoryCaptor.getValue();

        assertTrue(saved.getMemories().contains("助手的名字是Tom"));
        assertFalse(saved.getMemories().contains("助手的名字是Alice"));
        assertEquals(2, saved.getMemories().size());
    }

    @Test
    void manageLongTermMemory_ShouldCallLlmWithNonBlankUserMessage() {
        when(coreMemoryRepository.findByCustomerId("c2")).thenReturn(Optional.empty());
        when(langChainChatService.simpleChat(anyString(), anyString()))
                .thenReturn("{\"updated_memories\":[\"助手的名字是Tom\"]}")
                .thenReturn("summary");
        when(coreMemoryRepository.save(org.mockito.ArgumentMatchers.any(CustomerCoreMemory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        coreMemoryTools.manageLongTermMemory("c2", "助手的名字是Tom", null);

        ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(langChainChatService, org.mockito.Mockito.atLeastOnce()).simpleChat(anyString(), userMessageCaptor.capture());

        for (String userMessage : userMessageCaptor.getAllValues()) {
            assertFalse(userMessage == null || userMessage.isBlank());
        }
    }
}
