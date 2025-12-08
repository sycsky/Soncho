package com.example.aikef.dto.request;

import com.example.aikef.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateCustomerRequest(
        @NotBlank String name,
        @NotNull Channel primaryChannel,
        String email,
        String phone,
        String wechatOpenId,
        String whatsappId,
        String lineId,
        String telegramId,
        String facebookId,
        String avatarUrl,
        String location,
        String notes,
        Map<String, Object> customFields
) {
}
