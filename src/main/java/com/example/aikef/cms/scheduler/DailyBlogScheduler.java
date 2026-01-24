package com.example.aikef.cms.scheduler;

import com.example.aikef.cms.service.SeoArticleGeneratorService;
import com.example.aikef.llm.LangChainChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailyBlogScheduler {

    private final SeoArticleGeneratorService seoService;
    private final LangChainChatService llmService;

    // Run every day at 4:00 AM
    @Scheduled(cron = "0 0 4 * * ?")
    public void generateDailyPost() {
        log.info("Starting daily blog generation task...");
        try {
            // 1. Generate a topic
            String prompt = "Please suggest ONE trending and specific topic about 'AI Agents in Customer Service' or 'Workflow Automation' for a technical blog post. Just return the topic title, nothing else.";
            String topic = llmService.simpleChat("You are a content strategist.", prompt);
            
            log.info("Generated topic: {}", topic);
            
            // 2. Generate article
            seoService.generateAndPublishDailyArticle(topic);
            
            log.info("Daily blog generated successfully.");
        } catch (Exception e) {
            log.error("Failed to generate daily blog", e);
        }
    }
}
