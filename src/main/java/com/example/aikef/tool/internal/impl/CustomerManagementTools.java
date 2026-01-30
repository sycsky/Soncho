package com.example.aikef.tool.internal.impl;

import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.SpecialCustomer;
import com.example.aikef.service.ChatSessionService;
import com.example.aikef.service.SpecialCustomerService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class CustomerManagementTools {

    private final ChatSessionService chatSessionService;
    private final SpecialCustomerService specialCustomerService;
    private final com.example.aikef.service.CustomerService customerService;
    private final com.example.aikef.mapper.EntityMapper entityMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final com.example.aikef.service.CustomerTagService customerTagService;

    public CustomerManagementTools(ChatSessionService chatSessionService,
                                   SpecialCustomerService specialCustomerService,
                                   com.example.aikef.service.CustomerService customerService,
                                   com.example.aikef.mapper.EntityMapper entityMapper,
                                   com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                   com.example.aikef.service.CustomerTagService customerTagService) {
        this.chatSessionService = chatSessionService;
        this.specialCustomerService = specialCustomerService;
        this.customerService = customerService;
        this.entityMapper = entityMapper;
        this.objectMapper = objectMapper;
        this.customerTagService = customerTagService;
    }

    @Tool("Add manual tags to the current customer (e.g. VIP, Gift Buyer)")
    public String addCustomerTags(
            @P(value = "Current Session ID", required = true) String sessionId,
            @P(value = "Tags to add (comma separated)", required = true) String tags
    ) {
        try {
            ChatSession session = chatSessionService.findById(UUID.fromString(sessionId));
            Customer customer = session.getCustomer();
            if (customer == null) {
                return "Error: No customer associated with this session.";
            }

            if (tags == null || tags.isBlank()) {
                return "Error: Tags cannot be empty.";
            }

            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    customerTagService.addManualTag(customer.getId(), trimmedTag);
                }
            }

            return "Successfully added tags: " + tags;

        } catch (IllegalArgumentException e) {
            return "Error: Invalid Session ID format.";
        } catch (Exception e) {
            log.error("Failed to add customer tags via tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Remove manual tags from the current customer")
    public String removeCustomerTags(
            @P(value = "Current Session ID", required = true) String sessionId,
            @P(value = "Tags to remove (comma separated)", required = true) String tags
    ) {
        try {
            ChatSession session = chatSessionService.findById(UUID.fromString(sessionId));
            Customer customer = session.getCustomer();
            if (customer == null) {
                return "Error: No customer associated with this session.";
            }

            if (tags == null || tags.isBlank()) {
                return "Error: Tags cannot be empty.";
            }

            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    customerTagService.removeManualTag(customer.getId(), trimmedTag);
                }
            }

            return "Successfully removed tags: " + tags;

        } catch (IllegalArgumentException e) {
            return "Error: Invalid Session ID format.";
        } catch (Exception e) {
            log.error("Failed to remove customer tags via tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Get current customer's personal information (profile)")
    public String getSelfCustomerInfo(
            @P(value = "Current Session ID", required = true) String sessionId
    ) {
        try {
            ChatSession session = chatSessionService.findById(UUID.fromString(sessionId));
            Customer customer = session.getCustomer();
            if (customer == null) {
                return "Error: No customer associated with this session.";
            }
            
            // Convert to DTO using existing mapper
            com.example.aikef.dto.CustomerDto dto = entityMapper.toCustomerDto(customer);
            
            // Return as JSON string for better formatting
            return objectMapper.writeValueAsString(dto);

        } catch (IllegalArgumentException e) {
            return "Error: Invalid Session ID format.";
        } catch (Exception e) {
            log.error("Failed to get customer info", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Update current customer's personal information (profile)")
    public String updateSelfCustomerInfo(
            @P(value = "Current Session ID", required = true) String sessionId,
            @P(value = "New Name", required = false) String name,
            @P(value = "New Email", required = false) String email,
            @P(value = "New Phone", required = false) String phone,
            @P(value = "New Location", required = false) String location
    ) {
        try {
            ChatSession session = chatSessionService.findById(UUID.fromString(sessionId));
            Customer customer = session.getCustomer();
            if (customer == null) {
                return "Error: No customer associated with this session.";
            }

            com.example.aikef.dto.request.UpdateCustomerRequest updateRequest = new com.example.aikef.dto.request.UpdateCustomerRequest(
                    name,
                    null, // Primary channel not updatable via this tool usually
                    email,
                    phone,
                    null, // wechat
                    null, // whatsapp
                    null, // line
                    null, // telegram
                    null, // facebook
                    null, // avatar
                    location,
                    null, // notes
                    null, // customFields
                    null, // shopifyCustomerId
                    null, // shopifyCustomerInfo
                    null  // active
            );

            com.example.aikef.dto.CustomerDto updated = customerService.updateCustomer(customer.getId(), updateRequest);
            
            return "Successfully updated profile for " + updated.name();

        } catch (IllegalArgumentException e) {
             return "Error: " + e.getMessage();
        } catch (Exception e) {
            log.error("Failed to update customer info", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Set customer role using current session ID")
    public String setCustomerRole(
            @P(value = "Current Session ID", required = true) String sessionId,
            @P(value = "Role Code (e.g., SUPPLIER, LOGISTICS, PROMOTER, WAREHOUSE)", required = true) String roleCode
    ) {
        try {
            // 1. Find Session
            ChatSession session = chatSessionService.findById(UUID.fromString(sessionId));
            Customer customer = session.getCustomer();
            if (customer == null) {
                return "Error: No customer associated with this session.";
            }

            // 2. Assign Role (Service handles duplicate check/update)
            SpecialCustomer specialCustomer = specialCustomerService.assignRole(customer.getId(), roleCode);

            return String.format("Successfully assigned role '%s' (%s) to customer '%s'.", 
                    specialCustomer.getRole().getName(), 
                    roleCode, 
                    customer.getName());

        } catch (IllegalArgumentException e) {
            return "Error: Invalid Session ID format.";
        } catch (Exception e) {
            log.error("Failed to set customer role via tool", e);
            return "Error: " + e.getMessage();
        }
    }
}
