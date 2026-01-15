package com.example.aikef.tool.internal.impl;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TestTools {

    private static final Logger log = LoggerFactory.getLogger(TestTools.class);

    @Tool("Check current weather for a specific location. Example return: 'Sunny, 25°C'")
    public String checkWeather(
            @P(value = "City or location name (e.g., 'Beijing', 'New York')", required = true) String location,
            @P(value = "Temperature unit ('celsius' or 'fahrenheit')", required = false) String unit
    ) {
        log.info("Mocking weather check for location: {}, unit: {}", location, unit);
        
        // Mock logic
        String weatherCondition = "Sunny";
        int temp = 25;
        String tempUnit = "°C";

        if ("fahrenheit".equalsIgnoreCase(unit)) {
            temp = 77;
            tempUnit = "°F";
        }
        
        // Randomize slightly based on location string length to make it feel dynamic
        if (location != null && location.length() % 2 == 0) {
            weatherCondition = "Cloudy";
            temp -= 5;
        }

        return String.format("Current weather in %s: %s, %d%s", location, weatherCondition, temp, tempUnit);
    }

    @Tool("Send an email to a recipient")
    public String sendEmail(
            @P(value = "Recipient email address", required = true) String to,
            @P(value = "Email subject", required = true) String subject,
            @P(value = "Email body content", required = true) String body
    ) {
        log.info("Mocking sending email to: {}, subject: {}", to, subject);
        
        // Mock logic
        return String.format("Email sent successfully to %s with subject: '%s'", to, subject);
    }
}
