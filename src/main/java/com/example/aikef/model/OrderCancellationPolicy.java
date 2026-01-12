package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单取消政策实体
 */
@Entity
@Table(name = "order_cancellation_policies")
@Getter
@Setter
public class OrderCancellationPolicy extends AuditableEntity {

    /**
     * 政策名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 政策描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 订单创建后多少小时内可以取消
     * null 表示任何时间都可以取消（前提是未发货）
     */
    @Column(name = "cancellable_hours")
    private Integer cancellableHours;

    /**
     * 罚金百分比（0-100，最多一位小数）
     * null 或 0 表示无罚金
     */
    @Column(name = "penalty_percentage", precision = 4, scale = 1)
    private BigDecimal penaltyPercentage;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * 是否为默认政策
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 政策类型
     * FREE: 免费取消
     * WITH_PENALTY: 有罚金
     * NO_CANCELLATION: 不可取消
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 20)
    private PolicyType policyType = PolicyType.FREE;

    public enum PolicyType {
        FREE,           // 免费取消
        WITH_PENALTY,   // 有罚金
        NO_CANCELLATION // 不可取消
    }
}

