package com.example.aikef.model.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "procedural_memory")
@CompoundIndexes({
        @CompoundIndex(name = "idx_customer_scenario", def = "{'customerId': 1, 'scenarioKey': 1}"),
        @CompoundIndex(name = "idx_customer_status", def = "{'customerId': 1, 'status': 1, 'lastUpdated': -1}")
})
public class ProceduralMemory {

    @Id
    private String id;

    @Indexed
    private String customerId;

    @Indexed
    private String scenarioKey;

    private String title;

    private String status;

    private String memoryKind;

    private Boolean reusable;

    private String summary;

    private List<String> triggerPhrases;

    private Integer currentStep;

    private Integer successCount;

    private Long version;

    private Boolean archived;

    private Map<String, Object> slots;

    private List<Step> steps;

    private LocalDateTime lastUsedAt;

    private LocalDateTime completedAt;

    private LocalDateTime lastUpdated;

    private LocalDateTime createdAt;

    @Data
    public static class Step {
        private Integer index;
        private String goal;
        private String actionType;
        private String action;
        private String resultSummary;
        private String state;
    }
}
