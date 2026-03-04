package com.example.aikef.config;

import com.example.aikef.llm.LangChainChatService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiModelConfig {

    @Value("${langchain4j.open-ai.embedding-model.api-key:demo}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.embedding-model.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(LangChainChatService chatService) {
        // 1. 尝试从数据库动态加载
        EmbeddingModel dynamicModel = chatService.createEmbeddingModel();
        if (dynamicModel != null) {
            return dynamicModel;
        }

        // 2. 回退到配置文件
        return OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(openAiBaseUrl)
                .modelName("text-embedding-3-small")
                .build();
    }
}
