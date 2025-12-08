package com.example.aikef.dto.request;

/**
 * 创建会话分类请求
 */
public record CreateSessionCategoryRequest(
        String name,
        String description,
        String icon,
        String color,
        Integer sortOrder
) {
}

