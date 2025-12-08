package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 删除客户标签请求
 */
public record RemoveCustomerTagRequest(
        @NotBlank(message = "标签不能为空") String tag
) {
}
