package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新会话备注请求
 */
public record UpdateSessionNoteRequest(
        @NotBlank(message = "备注内容不能为空") String content
) {
}
