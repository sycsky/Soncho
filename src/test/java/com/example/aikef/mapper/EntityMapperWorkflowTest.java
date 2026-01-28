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
import org.springframework.security.crypto.password.PasswordEncoder;

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
    @Mock
    private PasswordEncoder passwordEncoder;

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
                workflowExecutionLogRepository,
                passwordEncoder
        );
    }

    @Test
    void toMessageDto_ShouldNotIncludeWorkflowExecutionDetails_EvenIfLogExists() {
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

        // Even if we mock the repository to return a log
        when(workflowExecutionLogRepository.findByMessageId(messageId)).thenReturn(Optional.of(log));

        // Act
        MessageDto dto = entityMapper.toMessageDto(message);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.agentMetadata());
        // The current implementation explicitly removed this injection
        assertFalse(dto.agentMetadata().containsKey("workflowExecution"));
    }
}
