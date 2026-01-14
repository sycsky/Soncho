package com.example.aikef.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsTrendItemDto {
    private String date; // YYYY-MM-DD
    private long conversations;
    private long aiMessages;
    private long humanMessages;
}
