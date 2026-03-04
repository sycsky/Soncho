package com.example.aikef.tool.internal;

import com.example.aikef.model.mongo.CustomerTool;
import com.example.aikef.repository.mongo.CustomerToolRepository;
import com.example.aikef.service.DockerSandboxService;
import com.example.aikef.service.ToolSecurityService;
import com.example.aikef.workflow.context.WorkflowContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.Runtime;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateLambdaTool {

    private final LambdaClient lambdaClient;
    private final CustomerToolRepository customerToolRepository;
    private final ToolSecurityService toolSecurityService;
    private final DockerSandboxService dockerSandboxService;

    @Value("${aws.lambda.role-arn:}")
    private String lambdaRoleArn;

    @Tool("Create a new Python tool using AWS Lambda. The code must be a valid Python script with a handler function.")
    public String createTool(
            @P("The name of the tool (e.g., 'calculate_tax')") String toolName,
            @P("Description of what the tool does") String description,
            @P("The Python code for the Lambda function. Must include 'def lambda_handler(event, context):'") String pythonCode,
            @P("JSON Schema for the tool parameters") String parametersJsonSchema,
            @P("Sample input JSON for testing the tool") String testInputJson,
            @ToolMemoryId WorkflowContext context) {

        if (context == null || context.getCustomerId() == null) {
            return "Error: Customer context is missing. Cannot create tool.";
        }
        
        // 1. Security Validation
        try {
            toolSecurityService.validateCode(pythonCode);
        } catch (SecurityException e) {
            log.warn("Tool creation rejected due to security violation: {}", e.getMessage());
            return "Error: Security Violation. " + e.getMessage();
        }

        String customerId = context.getCustomerId() != null ? context.getCustomerId().toString() : null;
        // Unique function name to avoid collisions
        String functionName = "agent_" + customerId + "_" + toolName + "_" + UUID.randomUUID().toString().substring(0, 8);
        // Clean function name to match AWS constraints (only letters, numbers, hyphens, or underscores)
        functionName = functionName.replaceAll("[^a-zA-Z0-9-_]", "_");

        try {
            // 2. Self-Testing (Docker Sandbox)
            log.info("Running self-test for tool '{}' with input: {}", toolName, testInputJson);
            
            String sandboxOutput = dockerSandboxService.executePython(pythonCode, testInputJson);
            
            if (sandboxOutput.contains("Error:") || sandboxOutput.contains("Traceback")) {
                log.warn("Self-test failed for tool '{}': {}", toolName, sandboxOutput);
                return "Error: Self-test failed. The code crashed or returned an error in the sandbox:\n" + sandboxOutput;
            }
            
            log.info("Self-test passed. Output: {}", sandboxOutput);

            // 3. Deploy to AWS Lambda
            if (lambdaRoleArn == null || lambdaRoleArn.isEmpty()) {
                // For local dev without role, maybe skip or use mock
                // But user asked for deployment.
                log.warn("AWS Lambda Role ARN is not configured. Cannot deploy to AWS.");
                return "Error: System configuration error (Missing Lambda Role ARN). Please contact admin.";
            }

            // Create Zip
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                ZipEntry entry = new ZipEntry("lambda_function.py");
                zos.putNextEntry(entry);
                zos.write(pythonCode.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            SdkBytes codeBytes = SdkBytes.fromByteArray(baos.toByteArray());

            CreateFunctionRequest createRequest = CreateFunctionRequest.builder()
                    .functionName(functionName)
                    .description("Agent Tool: " + description)
                    .runtime(Runtime.PYTHON3_9)
                    .handler("lambda_function.lambda_handler")
                    .role(lambdaRoleArn)
                    .code(FunctionCode.builder().zipFile(codeBytes).build())
                    .timeout(15) // 15 seconds timeout
                    .memorySize(128)
                    .build();

            log.info("Deploying Lambda function: {}", functionName);
            String lambdaArn = lambdaClient.createFunction(createRequest).functionArn();
            log.info("Lambda deployed successfully. ARN: {}", lambdaArn);
            
            // 4. Save Tool Metadata to MongoDB
            CustomerTool tool = new CustomerTool();
            tool.setCustomerId(customerId);
            tool.setToolName(toolName);
            tool.setDescription(description);
            tool.setParametersJsonSchema(parametersJsonSchema);
            tool.setLambdaArn(lambdaArn); 
            tool.setCode(pythonCode);
            tool.setCreatedAt(LocalDateTime.now());
            
            customerToolRepository.save(tool);

            return "Tool '" + toolName + "' created, tested, and deployed to AWS Lambda successfully. ARN: " + lambdaArn;

        } catch (Exception e) {
            log.error("Failed to create tool", e);
            return "Error creating tool: " + e.getMessage();
        }
    }
}
