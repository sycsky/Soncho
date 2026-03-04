package com.example.aikef.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class LambdaConfig {

    @Value("${aws.lambda.access-key:}")
    private String accessKey;

    @Value("${aws.lambda.secret-key:}")
    private String secretKey;

    @Value("${aws.lambda.region:us-east-1}")
    private String region;

    @Bean
    public LambdaClient lambdaClient() {
        if (accessKey == null || accessKey.isEmpty()) {
            return LambdaClient.builder()
                    .region(Region.of(region))
                    .build();
        }
        return LambdaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
