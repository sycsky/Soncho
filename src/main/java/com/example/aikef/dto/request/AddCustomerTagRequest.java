package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 添加客户标签请求
 */
public record AddCustomerTagRequest(
        @NotBlank(message = "标签不能为空") String tag
) {
}
