package com.example.aikef.tool.internal.impl;

import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.shopify.service.ShopifyGraphQLService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@Transactional
public class ContextTools {

    private final ObjectMapper objectMapper;
    private final CustomerRepository customerRepository;

    public ContextTools(ObjectMapper objectMapper, CustomerRepository customerRepository) {
        this.objectMapper = objectMapper.copy()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.customerRepository = customerRepository;
    }

    private Object unproxy(Object entity) {
        if (entity == null) {
            return null;
        }
        return Hibernate.unproxy(entity);
    }

    @Autowired
    private ShopifyGraphQLService  shopifyGraphQLService;

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
//            result.put("workflowId", ctx.getWorkflowId());
            result.put("sessionId", ctx.getSessionId());
//            result.put("query", ctx.getQuery());
//            result.put("intent", ctx.getIntent());
//            result.put("entities", ctx.getEntities());
            if(ctx.getVariables()!=null){
                result.put("eventData", ctx.getVariables().get("eventData"));
            }

            result.put("shopifyUrl",shopifyGraphQLService.getCurrentStore().getShopDomain());
            if (ctx.getCustomerId() != null) {
                Optional<Customer> customerOpt = customerRepository.findById(ctx.getCustomerId());
                if (customerOpt.isPresent()) {
                    result.put("customerInfo", unproxy(customerOpt.get()));
                } else {
                    result.put("customerInfo", unproxy(ctx.getCustomerInfo()));
                }
            } else {
                result.put("customerInfo", unproxy(ctx.getCustomerInfo()));
            }

            result.put("sessionMetadata", ctx.getSessionMetadata());
            result.put("nowTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

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
