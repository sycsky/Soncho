package com.example.aikef.tool.internal.impl;

import com.example.aikef.tool.annotation.AutoInjectTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AutoInjectTool
public class WebPageReaderTool {

    private final HttpClient httpClient;

    public WebPageReaderTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Tool("Read and extract text content from a specific web page URL. Use this after finding a URL from search results.")
    public String readWebPage(
            @P(value = "The URL of the web page to read", required = true) String url
    ) {
        if (url == null || url.isBlank()) {
            return "Error: URL cannot be empty.";
        }

        if (!url.startsWith("http")) {
            // Assume https if protocol missing
            url = "https://" + url;
        }

        log.info("Reading web page: {}", url);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                return "Error: Failed to fetch page. Status code: " + response.statusCode();
            }

            String html = response.body();
            return extractTextFromHtml(html);

        } catch (Exception e) {
            log.error("Error reading web page: {}", url, e);
            return "Error reading web page: " + e.getMessage();
        }
    }

    /**
     * Simple HTML text extractor (Regex-based, fallback since Jsoup is not available).
     * Removes script, style, and HTML tags.
     */
    private String extractTextFromHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        // 1. Remove script and style blocks entirely (including content inside)
        // (?s) enables dotall mode so . matches newlines
        String noScript = html.replaceAll("(?i)(?s)<script.*?>.*?</script>", " ");
        String noStyle = noScript.replaceAll("(?i)(?s)<style.*?>.*?</style>", " ");
        String noComments = noStyle.replaceAll("(?s)<!--.*?-->", " ");

        // 2. Remove all HTML tags
        String text = noComments.replaceAll("<[^>]+>", " ");

        // 3. Decode common HTML entities (basic subset)
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");

        // 4. Normalize whitespace (replace multiple spaces/newlines with single space)
        return text.replaceAll("\\s+", " ").trim();
    }
}
