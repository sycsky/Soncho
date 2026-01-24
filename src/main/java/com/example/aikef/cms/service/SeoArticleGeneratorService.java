package com.example.aikef.cms.service;

import com.example.aikef.cms.model.CmsArticle;
import com.example.aikef.cms.model.CmsArticleStatus;
import com.example.aikef.cms.model.CmsArticleType;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LangChainChatService.FieldSchemaDefinition;
import com.example.aikef.llm.LangChainChatService.StructuredOutputResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.example.aikef.llm.LangChainChatService.FieldSchemaDefinition.FieldType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeoArticleGeneratorService {

    private final LangChainChatService llmService;
    private final CmsService cmsService;
    private final ObjectMapper objectMapper;

    // Define the output schema for the blog post
    private static final List<FieldSchemaDefinition> BLOG_SCHEMA = List.of(
            new FieldSchemaDefinition("title", STRING, "The engaging title of the blog post", true, null, null, null),
            new FieldSchemaDefinition("slug", STRING, "URL friendly slug (lowercase, hyphens only)", true, null, null, null),
            new FieldSchemaDefinition("content", STRING, "The full content of the blog post in Markdown format. Include headers, bullet points, and code blocks if relevant.", true, null, null, null),
            new FieldSchemaDefinition("seo_title", STRING, "SEO optimized title (under 60 chars)", true, null, null, null),
            new FieldSchemaDefinition("seo_description", STRING, "SEO meta description (under 160 chars)", true, null, null, null),
            new FieldSchemaDefinition("keywords", ARRAY, "List of 5-10 SEO keywords", true, null, null, new FieldSchemaDefinition(null, STRING, null, false, null, null, null))
    );

    @Transactional
    public CmsArticle generateAndPublishDailyArticle(String topic) {
        log.info("Starting daily SEO article generation for topic: {}", topic);

        try {
            String systemPrompt = """
                    You are an expert technical content writer and SEO specialist for an AI Agent platform.
                    Your goal is to write a high-quality, engaging, and educational blog post about AI Agents, Workflow Automation, or Customer Service AI.
                    The content should be in Chinese (Simplified).
                    Make sure the tone is professional yet accessible.
                    Use Markdown formatting.
                    """;

            String userMessage = "Please write a blog post about: " + topic + ". Ensure it is up-to-date and provides value to developers and business owners.";

            // Use default model (null) or specify one if needed
            StructuredOutputResponse response = llmService.chatWithFieldDefinitions(
                    null, // Use default model
                    systemPrompt,
                    userMessage,
                    BLOG_SCHEMA,
                    "blog_post_schema",
                    0.7
            );

            if (!response.success()) {
                throw new RuntimeException("Failed to generate article: " + response.errorMessage());
            }

            JsonNode root = objectMapper.readTree(response.jsonResult());

            CmsArticle article = new CmsArticle();
            article.setTitle(root.path("title").asText());
            article.setSlug(root.path("slug").asText() + "-" + LocalDate.now().toString()); // Append date to ensure uniqueness
            article.setContent(root.path("content").asText());
            article.setSeoTitle(root.path("seo_title").asText());
            article.setSeoDescription(root.path("seo_description").asText());
            
            JsonNode keywordsNode = root.path("keywords");
            if (keywordsNode.isArray()) {
                StringBuilder keywords = new StringBuilder();
                keywordsNode.forEach(k -> {
                    if (!keywords.isEmpty()) keywords.append(",");
                    keywords.append(k.asText());
                });
                article.setSeoKeywords(keywords.toString());
            }

            article.setType(CmsArticleType.BLOG);
            article.setStatus(CmsArticleStatus.PUBLISHED); // Auto-publish
            
            return cmsService.createArticle(article);

        } catch (Exception e) {
            log.error("Error generating daily article", e);
            throw new RuntimeException(e);
        }
    }
}
