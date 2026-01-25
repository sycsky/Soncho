package com.example.aikef.dto.request;

import com.example.aikef.model.Channel;

import java.util.Map;

public record UpdateCustomerRequest(
        String name,
        Channel primaryChannel,
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
        Map<String, Object> customFields,
        String shopifyCustomerId,
        Map<String, Object> shopifyCustomerInfo,
        Boolean active
) {
}
