package com.example.aikef.dto.request;

import com.example.aikef.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 快速创建客户并获取 Token 的请求
 */
public record QuickCustomerRequest(
        @NotBlank String name,
        @NotNull Channel channel,
        String email,           // 可选，邮箱
        String phone,           // 可选，手机号
        String channelUserId,   // 可选，渠道用户ID（如微信 openId）
        /**
         * 会话元数据（可选）
         * 可包含以下字段：
         * - categoryId: 会话分类ID (String，UUID格式)
         * - source: 来源渠道
         * - referrer: 来源页面
         * - device: 设备信息
         * - 其他自定义字段...
         */
        Map<String, Object> metadata
) {
}
