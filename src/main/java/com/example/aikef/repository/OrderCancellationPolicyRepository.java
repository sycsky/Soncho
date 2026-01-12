package com.example.aikef.repository;

import com.example.aikef.model.OrderCancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderCancellationPolicyRepository extends JpaRepository<OrderCancellationPolicy, UUID> {

    /**
     * 查找启用的政策
     */
    List<OrderCancellationPolicy> findByEnabledTrueOrderBySortOrderAsc();

    /**
     * 查找默认政策
     */
    Optional<OrderCancellationPolicy> findByIsDefaultTrueAndEnabledTrue();

    /**
     * 查找所有政策（按排序顺序）
     */
    List<OrderCancellationPolicy> findAllByOrderBySortOrderAsc();
}

