package com.example.aikef.model.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Document(collection = "customer_core_memory")
public class CustomerCoreMemory {

    @Id
    private String id;

    @Indexed(unique = true)
    private String customerId;

    /**
     * User profile information extracted from conversations.
     * e.g., "name": "John", "preference": "loves coffee", "language": "en"
     */
    private Map<String, Object> profile;

    private List<String> memories;

    /**
     * A summarized text version of the profile for injection into System Prompt.
     */
    private String summary;

    private LocalDateTime lastUpdated;

    private LocalDateTime createdAt;
}
