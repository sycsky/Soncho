package com.example.aikef.service;

import com.example.aikef.model.OrderCancellationPolicy;
import com.example.aikef.repository.OrderCancellationPolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 订单取消政策服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderCancellationPolicyService {

    private final OrderCancellationPolicyRepository policyRepository;

    /**
     * 获取所有政策
     */
    public List<OrderCancellationPolicy> getAllPolicies() {
        return policyRepository.findAllByOrderBySortOrderAsc();
    }

    /**
     * 获取启用的政策
     */
    public List<OrderCancellationPolicy> getEnabledPolicies() {
        return policyRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    /**
     * 获取默认政策
     */
    public OrderCancellationPolicy getDefaultPolicy() {
        return policyRepository.findByIsDefaultTrueAndEnabledTrue()
                .orElseThrow(() -> new EntityNotFoundException("未找到默认取消政策"));
    }

    /**
     * 根据ID获取政策
     */
    public OrderCancellationPolicy getPolicyById(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("取消政策不存在"));
    }

    /**
     * 创建政策
     */
    @Transactional
    public OrderCancellationPolicy createPolicy(OrderCancellationPolicy policy) {
        // 验证罚金百分比
        if (policy.getPenaltyPercentage() != null) {
            validatePenaltyPercentage(policy.getPenaltyPercentage());
        }

        // 如果设置为默认，取消其他默认政策
        if (policy.isDefault()) {
            setOthersNonDefault();
        }

        return policyRepository.save(policy);
    }

    /**
     * 更新政策
     */
    @Transactional
    public OrderCancellationPolicy updatePolicy(UUID id, OrderCancellationPolicy updatedPolicy) {
        OrderCancellationPolicy policy = getPolicyById(id);

        policy.setName(updatedPolicy.getName());
        policy.setDescription(updatedPolicy.getDescription());
        policy.setCancellableHours(updatedPolicy.getCancellableHours());
        policy.setPenaltyPercentage(updatedPolicy.getPenaltyPercentage());
        policy.setEnabled(updatedPolicy.isEnabled());
        policy.setPolicyType(updatedPolicy.getPolicyType());
        policy.setSortOrder(updatedPolicy.getSortOrder());

        // 验证罚金百分比
        if (policy.getPenaltyPercentage() != null) {
            validatePenaltyPercentage(policy.getPenaltyPercentage());
        }

        // 如果设置为默认，取消其他默认政策
        if (updatedPolicy.isDefault() && !policy.isDefault()) {
            setOthersNonDefault();
            policy.setDefault(true);
        } else if (!updatedPolicy.isDefault() && policy.isDefault()) {
            policy.setDefault(false);
        }

        return policyRepository.save(policy);
    }

    /**
     * 删除政策
     */
    @Transactional
    public void deletePolicy(UUID id) {
        OrderCancellationPolicy policy = getPolicyById(id);
        
        if (policy.isDefault()) {
            throw new IllegalStateException("无法删除默认政策，请先设置其他政策为默认");
        }
        
        policyRepository.delete(policy);
    }

    /**
     * 设置默认政策
     */
    @Transactional
    public void setDefaultPolicy(UUID id) {
        OrderCancellationPolicy policy = getPolicyById(id);
        
        if (!policy.isEnabled()) {
            throw new IllegalStateException("无法将未启用的政策设置为默认");
        }
        
        setOthersNonDefault();
        policy.setDefault(true);
        policyRepository.save(policy);
    }

    /**
     * 使用阶梯匹配逻辑检查订单是否可以取消
     * 
     * 匹配规则：
     * 1. 遍历所有启用的政策，检查订单时间是否超过了政策的时间限制
     * 2. 如果超过了（orderAgeHours > cancellableHours），记录该政策，后面的覆盖前面的
     * 3. 如果没超过，说明还在该政策的保护期内，跳过
     * 4. 循环结束后：
     *    - penaltyPolicy == null：没有超过任何政策，免费取消
     *    - penaltyPolicy != null：根据该政策的类型决定（FREE=免费，WITH_PENALTY=罚金，NO_CANCELLATION=不可取消）
     * 
     * @param orderCreatedAt 订单创建时间
     * @return 取消结果
     */
    public CancellationCheckResult checkCancellationWithLadder(Instant orderCreatedAt) {
        // 获取所有启用的政策，按 sortOrder 排序
        List<OrderCancellationPolicy> enabledPolicies = getEnabledPolicies();
        
        if (enabledPolicies.isEmpty()) {
            return new CancellationCheckResult(
                true,
                "No specific cancellation policy configured, allowing cancellation by default",
                BigDecimal.ZERO,
                null
            );
        }
        
        Instant now = Instant.now();
        long orderAgeHours = ChronoUnit.HOURS.between(orderCreatedAt, now);
        
        // 记录匹配的惩罚政策
        OrderCancellationPolicy penaltyPolicy = null;
        
        // 单次循环遍历所有启用的政策
        for (OrderCancellationPolicy policy : enabledPolicies) {


            //免费取消没有时间限制，遇到则直接使用免费取消
            if (policy.getPolicyType() == OrderCancellationPolicy.PolicyType.FREE) {
                return new CancellationCheckResult(
                        true,
                        String.format("Can cancel for free according to policy '%s'", policy.getName()),
                        BigDecimal.ZERO,
                        policy
                );
            }
            // 检查是否超过了该政策的时间限制
            if (policy.getCancellableHours() != null && orderAgeHours > policy.getCancellableHours()) {
                // 超过了该政策的时间，记录该政策（后面的会覆盖前面的）
                penaltyPolicy = policy;
            }

        }
        
        // 循环结束，判断是否有惩罚政策
        if (penaltyPolicy == null) {
            // 没有超过任何政策的时间限制，完全免费取消
            return new CancellationCheckResult(
                true,
                "Order is within all policy protection periods, can cancel for free",
                BigDecimal.ZERO,
                null
            );
        }
        
        if (penaltyPolicy.getPolicyType() == OrderCancellationPolicy.PolicyType.NO_CANCELLATION) {
            // 不可取消
            return new CancellationCheckResult(
                false,
                String.format("Order cannot be cancelled according to policy '%s'", penaltyPolicy.getName()),
                null,
                penaltyPolicy
            );
        }
        
        // WITH_PENALTY - 需要支付罚金
        BigDecimal penaltyPercentage = penaltyPolicy.getPenaltyPercentage() != null ? 
            penaltyPolicy.getPenaltyPercentage() : BigDecimal.ZERO;
        
        return new CancellationCheckResult(
            true,
            String.format("Can cancel with %.1f%% penalty according to policy '%s'",
                penaltyPercentage, penaltyPolicy.getName()),
            penaltyPercentage,
            penaltyPolicy
        );
    }
    
    /**
     * 检查订单是否可以取消（旧方法，保留兼容性）
     * @deprecated 使用 checkCancellationWithLadder 代替
     */
    @Deprecated
    public CancellationCheckResult checkCancellation(Instant orderCreatedAt, OrderCancellationPolicy policy) {
        // 检查政策类型
        if (policy.getPolicyType() == OrderCancellationPolicy.PolicyType.NO_CANCELLATION) {
            return new CancellationCheckResult(
                false, 
                "Order cannot be cancelled according to the cancellation policy",
                null,
                policy
            );
        }

        // 检查时间限制
        if (policy.getCancellableHours() != null) {
            Instant cancellableUntil = orderCreatedAt.plus(policy.getCancellableHours(), ChronoUnit.HOURS);
            Instant now = Instant.now();
            
            if (now.isAfter(cancellableUntil)) {
                return new CancellationCheckResult(
                    false,
                    String.format("Order cannot be cancelled after %d hours", policy.getCancellableHours()),
                    null,
                    policy
                );
            }
        }

        // 计算罚金
        BigDecimal penaltyPercentage = policy.getPenaltyPercentage() != null ? 
            policy.getPenaltyPercentage() : BigDecimal.ZERO;

        return new CancellationCheckResult(
            true,
            policy.getPolicyType() == OrderCancellationPolicy.PolicyType.FREE ?
                "Can cancel for free" :
                String.format("Can cancel with %.1f%% penalty", penaltyPercentage),
            penaltyPercentage,
            policy
        );
    }

    /**
     * 取消结果
     */
    public record CancellationCheckResult(
        boolean canCancel,
        String message,
        BigDecimal penaltyPercentage,
        OrderCancellationPolicy matchedPolicy
    ) {}

    /**
     * 验证罚金百分比
     */
    private void validatePenaltyPercentage(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || 
            percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("罚金百分比必须在0-100之间");
        }

        // 检查小数位数不超过1位
        if (percentage.scale() > 1) {
            throw new IllegalArgumentException("罚金百分比最多只能有一位小数");
        }
    }

    /**
     * 将其他政策设置为非默认
     */
    private void setOthersNonDefault() {
        List<OrderCancellationPolicy> allPolicies = policyRepository.findAll();
        for (OrderCancellationPolicy p : allPolicies) {
            if (p.isDefault()) {
                p.setDefault(false);
                policyRepository.save(p);
            }
        }
    }
    
    /**
     * 上移政策（减小sortOrder）
     */
    @Transactional
    public void movePolicyUp(UUID id) {
        OrderCancellationPolicy policy = getPolicyById(id);
        List<OrderCancellationPolicy> allPolicies = getAllPolicies();
        
        // 找到当前政策和上一个政策
        OrderCancellationPolicy previousPolicy = null;
        for (OrderCancellationPolicy p : allPolicies) {
            if (p.getId().equals(id)) {
                break;
            }
            previousPolicy = p;
        }
        
        if (previousPolicy != null) {
            // 交换排序
            int tempOrder = policy.getSortOrder();
            policy.setSortOrder(previousPolicy.getSortOrder());
            previousPolicy.setSortOrder(tempOrder);
            
            policyRepository.save(policy);
            policyRepository.save(previousPolicy);
        }
    }
    
    /**
     * 下移政策（增大sortOrder）
     */
    @Transactional
    public void movePolicyDown(UUID id) {
        OrderCancellationPolicy policy = getPolicyById(id);
        List<OrderCancellationPolicy> allPolicies = getAllPolicies();
        
        // 找到当前政策和下一个政策
        boolean foundCurrent = false;
        OrderCancellationPolicy nextPolicy = null;
        for (OrderCancellationPolicy p : allPolicies) {
            if (foundCurrent) {
                nextPolicy = p;
                break;
            }
            if (p.getId().equals(id)) {
                foundCurrent = true;
            }
        }
        
        if (nextPolicy != null) {
            // 交换排序
            int tempOrder = policy.getSortOrder();
            policy.setSortOrder(nextPolicy.getSortOrder());
            nextPolicy.setSortOrder(tempOrder);
            
            policyRepository.save(policy);
            policyRepository.save(nextPolicy);
        }
    }
}

