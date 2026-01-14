package com.example.aikef.service;

import com.example.aikef.dto.analytics.AnalyticsSummaryDto;
import com.example.aikef.dto.analytics.AnalyticsTrendItemDto;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.enums.MessageType;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getSummary(String tenantId, Instant start, Instant end) {
        // Basic Counts
        long totalConversations = chatSessionRepository.countByCreatedAtBetweenAndTenantId(start, end, tenantId);
        long totalMessages = messageRepository.countByCreatedAtBetweenAndTenantId(start, end, tenantId);
        long aiMessages = messageRepository.countByCreatedAtBetweenAndTenantIdAndSenderType(start, end, tenantId, SenderType.AI);
        long humanMessages = messageRepository.countByCreatedAtBetweenAndTenantIdAndSenderType(start, end, tenantId, SenderType.AGENT);
        
        long orderLookups = messageRepository.countByCreatedAtBetweenAndTenantIdAndMessageType(start, end, tenantId, MessageType.CARD_ORDER);
        Map<String, Long> orderActions = new HashMap<>();
        orderActions.put("lookup", orderLookups);

        // Advanced Analytics: Fetch sessions to aggregate Tags and Status
        List<ChatSession> sessions = chatSessionRepository.findByCreatedAtBetweenAndTenantId(start, end, tenantId);

        // 1. Session Status Distribution
        Map<String, Long> sessionStatusDistribution = sessions.stream()
                .map(s -> s.getStatus().name())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 2. Top Tags (Combine manual tags and AI tags)
        Map<String, Long> tagCounts = new HashMap<>();
        for (ChatSession session : sessions) {
            Customer customer = session.getCustomer();
            if (customer != null) {
                if (customer.getTags() != null) {
                    customer.getTags().forEach(tag -> tagCounts.merge(tag, 1L, Long::sum));
                }
                if (customer.getAiTags() != null) {
                    customer.getAiTags().forEach(tag -> tagCounts.merge(tag, 1L, Long::sum));
                }
            }
        }
        
        // Sort and limit top 10 tags
        Map<String, Long> topTags = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return AnalyticsSummaryDto.builder()
                .totalConversations(totalConversations)
                .totalMessages(totalMessages)
                .aiMessages(aiMessages)
                .humanMessages(humanMessages)
                .aiHandledCount(0) // Placeholder
                .humanHandledCount(0)
                .orderActions(orderActions)
                .sessionStatusDistribution(sessionStatusDistribution)
                .topTags(topTags)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsTrendItemDto> getTrend(String tenantId, Instant start, Instant end) {
        List<AnalyticsTrendItemDto> trend = new ArrayList<>();
        
        // Iterate by day
        LocalDate startDate = start.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = end.atZone(ZoneId.systemDefault()).toLocalDate();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            // Clip to request range
            if (dayStart.isBefore(start)) dayStart = start;
            if (dayEnd.isAfter(end)) dayEnd = end;
            
            if (dayStart.isAfter(dayEnd)) continue;

            long conversations = chatSessionRepository.countByCreatedAtBetweenAndTenantId(dayStart, dayEnd, tenantId);
            long aiMsgs = messageRepository.countByCreatedAtBetweenAndTenantIdAndSenderType(dayStart, dayEnd, tenantId, SenderType.AI);
            long humanMsgs = messageRepository.countByCreatedAtBetweenAndTenantIdAndSenderType(dayStart, dayEnd, tenantId, SenderType.AGENT);
            
            trend.add(AnalyticsTrendItemDto.builder()
                    .date(date.toString())
                    .conversations(conversations)
                    .aiMessages(aiMsgs)
                    .humanMessages(humanMsgs)
                    .build());
        }
        
        return trend;
    }
}
