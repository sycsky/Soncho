package com.example.aikef.dto.request;

/**
 * 更新会话分类请求
 */
public record UpdateSessionCategoryRequest(
        String name,
        String description,
        String icon,
        String color,
        Integer sortOrder,
        Boolean enabled
) {
}

