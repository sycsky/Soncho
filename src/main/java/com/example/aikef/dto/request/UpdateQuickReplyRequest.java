package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新快捷回复请求
 */
public record UpdateQuickReplyRequest(
        @NotBlank(message = "标签不能为空") String label,
        @NotBlank(message = "内容不能为空") String text,
        String category
) {
}
