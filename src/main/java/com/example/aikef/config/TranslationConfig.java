package com.example.aikef.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.translate.TranslateClient;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS Translate 翻译服务配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "translation")
public class TranslationConfig {

    /**
     * 是否启用翻译服务
     */
    private boolean enabled = true;

    /**
     * AWS 配置
     */
    private AwsConfig aws = new AwsConfig();

    /**
     * 支持的目标语言列表
     */
    private List<TargetLanguage> targetLanguages = new ArrayList<>();

    /**
     * 默认系统语言（客服和AI使用的语言）
     */
    private String defaultSystemLanguage = "zh-CN";

    @Data
    public static class AwsConfig {
        private String accessKey;
        private String secretKey;
        private String region = "us-east-1";
    }

    @Data
    public static class TargetLanguage {
        /**
         * 语言代码（如 zh-TW, en, ja）
         */
        private String code;
        /**
         * 语言名称（如 中文繁体, English, 日本語）
         */
        private String name;
    }

    /**
     * 创建 AWS Translate 客户端
     */
    @Bean
    public TranslateClient translateClient() {
        if (!enabled || aws.getAccessKey() == null || aws.getAccessKey().isBlank()) {
            return null;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                aws.getAccessKey(),
                aws.getSecretKey()
        );

        return TranslateClient.builder()
                .region(Region.of(aws.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    /**
     * 获取所有目标语言代码
     */
    public List<String> getTargetLanguageCodes() {
        return targetLanguages.stream()
                .map(TargetLanguage::getCode)
                .toList();
    }

    /**
     * 检查语言代码是否在支持列表中
     */
    public boolean isLanguageSupported(String languageCode) {
        if (languageCode == null) return false;
        return targetLanguages.stream()
                .anyMatch(lang -> lang.getCode().equalsIgnoreCase(languageCode));
    }
}

