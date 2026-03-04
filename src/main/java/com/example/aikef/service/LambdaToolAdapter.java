package com.example.aikef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LambdaToolAdapter {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    /**
     * Invoke an AWS Lambda function dynamically.
     *
     * @param lambdaArn The ARN of the Lambda function to invoke.
     * @param arguments The arguments to pass to the function (as a JSON object).
     * @return The result from the Lambda function (as a String).
     */
    public String execute(String lambdaArn, Map<String, Object> arguments) {
        try {
            String payload = objectMapper.writeValueAsString(arguments);
            log.info("Invoking Lambda: {} with payload: {}", lambdaArn, payload);

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(lambdaArn)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);
            String result = response.payload().asUtf8String();

            if (response.functionError() != null) {
                log.error("Lambda execution error: {}", response.functionError());
                return "Error executing tool: " + response.functionError() + " Details: " + result;
            }

            log.info("Lambda execution successful: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Failed to execute Lambda tool: {}", lambdaArn, e);
            return "Error executing tool: " + e.getMessage();
        }
    }
}
