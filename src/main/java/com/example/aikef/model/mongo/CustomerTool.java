package com.example.aikef.model.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Document(collection = "customer_tools")
public class CustomerTool {

    @Id
    private String id;

    @Indexed
    private String customerId;

    @Indexed
    private String toolName;

    private String description;

    /**
     * JSON Schema for tool parameters.
     */
    private String parametersJsonSchema;

    /**
     * AWS Lambda ARN to invoke.
     */
    private String lambdaArn;

    /**
     * Source code for the tool (e.g. Python script).
     */
    private String code;

    private LocalDateTime createdAt;
}
