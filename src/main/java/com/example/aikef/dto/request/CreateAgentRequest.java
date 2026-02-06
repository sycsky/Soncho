package com.example.aikef.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private UUID roleId;

    private String language; // 可选，客服使用的语言代码（如 zh-CN, en, ja）

    private String tenantId; // 可选，仅在 SAAS 模式下管理员创建租户管理员时使用
}
