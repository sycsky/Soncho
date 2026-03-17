package com.example.aikef.tool.internal.impl;

import com.example.aikef.tool.annotation.AutoInjectTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@AutoInjectTool
public class WebSearchTool {

    // Inject API key if available, otherwise use mock
    @Value("${ai.search.api-key:}")
    private String searchApiKey;

    @Tool("Search the internet for real-time information, news, or specific data.")
    public String searchWeb(
            @P(value = "Search query", required = true) String query
    ) {
        log.info("Executing web search for: {}", query);
        
        if (searchApiKey == null || searchApiKey.isBlank()) {
            // Mock implementation when no API key is configured
            return mockSearch(query);
        }

        // TODO: Implement real search (e.g., Google Custom Search, Bing, SerpApi)
        // For now, we still return mock to prevent errors until real implementation is added
        return mockSearch(query) + "\n(Note: Real search API not yet connected)";
    }

    private String mockSearch(String query) {
        return String.format("""
                [Search Results for '%s']
                1. Official Documentation for %s
                   Summary: Detailed guide and reference...
                   Source: https://example.com/docs
                
                2. Latest News about %s
                   Summary: Recent updates indicate that...
                   Source: https://news.example.com/latest
                
                3. Community Forum - %s Discussion
                   Summary: Users are discussing best practices...
                   Source: https://forum.example.com/topic
                """, query, query, query, query);
    }
}
