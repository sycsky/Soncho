package com.example.aikef.dto.request;

import com.example.aikef.model.OrderCancellationPolicy;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SaveOrderCancellationPolicyRequest(
        @NotBlank(message = "政策名称不能为空")
        @Size(max = 100, message = "政策名称不能超过100个字符")
        String name,

        @Size(max = 500, message = "政策描述不能超过500个字符")
        String description,

        @Min(value = 0, message = "可取消小时数不能为负数")
        Integer cancellableHours,

        @DecimalMin(value = "0.0", message = "罚金百分比不能小于0")
        @DecimalMax(value = "100.0", message = "罚金百分比不能超过100")
        @Digits(integer = 3, fraction = 1, message = "罚金百分比最多一位小数")
        BigDecimal penaltyPercentage,

        @NotNull(message = "是否启用不能为空")
        Boolean enabled,

        Boolean isDefault,

        Integer sortOrder,

        @NotNull(message = "政策类型不能为空")
        OrderCancellationPolicy.PolicyType policyType
) {
}

