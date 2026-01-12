package com.example.aikef.controller;

import com.example.aikef.dto.OrderCancellationPolicyDto;
import com.example.aikef.dto.request.SaveOrderCancellationPolicyRequest;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.OrderCancellationPolicy;
import com.example.aikef.service.OrderCancellationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/order-cancellation-policies")
@RequiredArgsConstructor
public class OrderCancellationPolicyController {

    private final OrderCancellationPolicyService policyService;
    private final EntityMapper entityMapper;

    /**
     * 获取所有取消政策
     */
    @GetMapping
    public ResponseEntity<List<OrderCancellationPolicyDto>> getAllPolicies() {
        List<OrderCancellationPolicy> policies = policyService.getAllPolicies();
        List<OrderCancellationPolicyDto> dtos = policies.stream()
                .map(entityMapper::toOrderCancellationPolicyDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 获取启用的取消政策
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<OrderCancellationPolicyDto>> getEnabledPolicies() {
        List<OrderCancellationPolicy> policies = policyService.getEnabledPolicies();
        List<OrderCancellationPolicyDto> dtos = policies.stream()
                .map(entityMapper::toOrderCancellationPolicyDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 获取默认取消政策
     */
    @GetMapping("/default")
    public ResponseEntity<OrderCancellationPolicyDto> getDefaultPolicy() {
        OrderCancellationPolicy policy = policyService.getDefaultPolicy();
        return ResponseEntity.ok(entityMapper.toOrderCancellationPolicyDto(policy));
    }

    /**
     * 根据ID获取取消政策
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderCancellationPolicyDto> getPolicyById(@PathVariable UUID id) {
        OrderCancellationPolicy policy = policyService.getPolicyById(id);
        return ResponseEntity.ok(entityMapper.toOrderCancellationPolicyDto(policy));
    }

    /**
     * 创建取消政策
     */
    @PostMapping
    public ResponseEntity<OrderCancellationPolicyDto> createPolicy(
            @Valid @RequestBody SaveOrderCancellationPolicyRequest request) {
        
        OrderCancellationPolicy policy = new OrderCancellationPolicy();
        policy.setName(request.name());
        policy.setDescription(request.description());
        policy.setCancellableHours(request.cancellableHours());
        policy.setPenaltyPercentage(request.penaltyPercentage());
        policy.setEnabled(request.enabled());
        policy.setDefault(request.isDefault() != null && request.isDefault());
        policy.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        policy.setPolicyType(request.policyType());

        OrderCancellationPolicy created = policyService.createPolicy(policy);
        return ResponseEntity.ok(entityMapper.toOrderCancellationPolicyDto(created));
    }

    /**
     * 更新取消政策
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderCancellationPolicyDto> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody SaveOrderCancellationPolicyRequest request) {
        
        OrderCancellationPolicy policy = new OrderCancellationPolicy();
        policy.setName(request.name());
        policy.setDescription(request.description());
        policy.setCancellableHours(request.cancellableHours());
        policy.setPenaltyPercentage(request.penaltyPercentage());
        policy.setEnabled(request.enabled());
        policy.setDefault(request.isDefault() != null && request.isDefault());
        policy.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        policy.setPolicyType(request.policyType());

        OrderCancellationPolicy updated = policyService.updatePolicy(id, policy);
        return ResponseEntity.ok(entityMapper.toOrderCancellationPolicyDto(updated));
    }

    /**
     * 删除取消政策
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePolicy(@PathVariable UUID id) {
        policyService.deletePolicy(id);
        return ResponseEntity.ok(Map.of("message", "取消政策已删除"));
    }

    /**
     * 设置默认取消政策
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<Map<String, String>> setDefaultPolicy(@PathVariable UUID id) {
        policyService.setDefaultPolicy(id);
        return ResponseEntity.ok(Map.of("message", "默认取消政策已设置"));
    }
    
    /**
     * 上移政策
     */
    @PutMapping("/{id}/move-up")
    public ResponseEntity<Map<String, String>> movePolicyUp(@PathVariable UUID id) {
        policyService.movePolicyUp(id);
        return ResponseEntity.ok(Map.of("message", "政策已上移"));
    }
    
    /**
     * 下移政策
     */
    @PutMapping("/{id}/move-down")
    public ResponseEntity<Map<String, String>> movePolicyDown(@PathVariable UUID id) {
        policyService.movePolicyDown(id);
        return ResponseEntity.ok(Map.of("message", "政策已下移"));
    }
}

