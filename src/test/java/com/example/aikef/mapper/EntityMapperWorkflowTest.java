package com.example.aikef.mapper;

import com.example.aikef.dto.MessageDto;
import com.example.aikef.model.Message;
import com.example.aikef.model.WorkflowExecutionLog;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.repository.SessionGroupMappingRepository;
import com.example.aikef.repository.SpecialCustomerRepository;
import com.example.aikef.repository.WorkflowExecutionLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EntityMapperWorkflowTest {

    @Mock
    private SessionGroupMappingRepository sessionGroupMappingRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private SpecialCustomerRepository specialCustomerRepository;
    @Mock
    private WorkflowExecutionLogRepository workflowExecutionLogRepository;

    private EntityMapper entityMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        entityMapper = new EntityMapper(
                sessionGroupMappingRepository,
                messageRepository,
                agentRepository,
                objectMapper,
                specialCustomerRepository,
                workflowExecutionLogRepository
        );
    }

    @Test
    void toMessageDto_ShouldIncludeWorkflowExecutionDetails_WhenLogExists() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        Message message = new Message();
        message.setId(messageId);
        message.setText("Test message");
        message.setAttachments(new java.util.ArrayList<>()); // Initialize list to avoid NPE in stream
        message.setAgentMetadata(new HashMap<>()); // Initialize map

        WorkflowExecutionLog log = new WorkflowExecutionLog();
        log.setId(UUID.randomUUID());
        log.setStatus("SUCCESS");
        log.setNodeDetails("[]");
        log.setToolExecutionChain("[]");
        log.setDurationMs(100L);

        when(workflowExecutionLogRepository.findByMessageId(messageId)).thenReturn(Optional.of(log));

        // Act
        MessageDto dto = entityMapper.toMessageDto(message);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.agentMetadata());
        assertTrue(dto.agentMetadata().containsKey("workflowExecution"));

        @SuppressWarnings("unchecked")
        Map<String, Object> workflowInfo = (Map<String, Object>) dto.agentMetadata().get("workflowExecution");
        assertEquals(log.getId(), workflowInfo.get("executionId"));
        assertEquals("SUCCESS", workflowInfo.get("status"));
        assertEquals(100L, workflowInfo.get("durationMs"));
    }

    @Test
    void toMessageDto_ShouldNotIncludeWorkflowExecutionDetails_WhenLogDoesNotExist() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        Message message = new Message();
        message.setId(messageId);
        message.setText("Test message");
        message.setAttachments(new java.util.ArrayList<>());
        message.setAgentMetadata(new HashMap<>());

        when(workflowExecutionLogRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        // Act
        MessageDto dto = entityMapper.toMessageDto(message);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.agentMetadata());
        assertFalse(dto.agentMetadata().containsKey("workflowExecution"));
    }
}
