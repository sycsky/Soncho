package com.example.aikef.service;

import com.example.aikef.dto.response.AiRewriteResponse;
import com.example.aikef.dto.response.AiSuggestTagsResponse;
import com.example.aikef.dto.response.AiSummaryResponse;
import com.example.aikef.model.Message;
import com.example.aikef.repository.MessageRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiKnowledgeService {

    private final MessageRepository messageRepository;

    public AiKnowledgeService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public AiSummaryResponse summarize(String sessionId) {
        UUID id = UUID.fromString(sessionId);
        List<Message> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(id);
        String summary = messages.stream()
                .map(message -> DateTimeFormatter.ISO_INSTANT.format(message.getCreatedAt()) + " - " + message.getText())
                .collect(Collectors.joining("\n"));
        if (summary.isBlank()) {
            summary = "暂无可用内容";
        }
        return new AiSummaryResponse(summary);
    }

    public AiRewriteResponse rewrite(String text) {
        String rewritten = "建议回复：" + text.strip();
        return new AiRewriteResponse(rewritten);
    }

    public AiSuggestTagsResponse suggestTags(String sessionId) {
        UUID id = UUID.fromString(sessionId);
        List<Message> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(id);
        String allText = messages.stream()
                .map(message -> message.getText() == null ? "" : message.getText().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        Set<String> tags = TagHeuristics.suggest(allText);
        return new AiSuggestTagsResponse(List.copyOf(tags));
    }

    private static final class TagHeuristics {
        private TagHeuristics() {
        }

        static Set<String> suggest(String corpus) {
            if (corpus == null || corpus.isBlank()) {
                return Set.of("待分类");
            }
            Set<String> result = new java.util.LinkedHashSet<>();
            if (corpus.contains("退款") || corpus.contains("退货")) {
                result.add("售后");
            }
            if (corpus.contains("支付") || corpus.contains("账单")) {
                result.add("账单问题");
            }
            if (corpus.contains("物流") || corpus.contains("快递")) {
                result.add("物流");
            }
            if (result.isEmpty()) {
                result.add("一般咨询");
            }
            return result;
        }
    }
}
