package com.example.aikef.controller;

import com.example.aikef.dto.CustomerDto;
import com.example.aikef.dto.request.AddCustomerTagRequest;
import com.example.aikef.dto.request.RemoveCustomerTagRequest;
import com.example.aikef.service.CustomerTagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 客户标签管理 API
 * 区分手动标签和AI标签
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/tags")
public class CustomerTagController {

    private final CustomerTagService customerTagService;

    public CustomerTagController(CustomerTagService customerTagService) {
        this.customerTagService = customerTagService;
    }

    /**
     * 获取客户所有标签（包括手动和AI标签）
     */
    @GetMapping
    public CustomerDto getAllTags(@PathVariable UUID customerId) {
        return customerTagService.getAllTags(customerId);
    }

    /**
     * 获取手动标签
     */
    @GetMapping("/manual")
    public List<String> getManualTags(@PathVariable UUID customerId) {
        return customerTagService.getManualTags(customerId);
    }

    /**
     * 获取AI标签
     */
    @GetMapping("/ai")
    public List<String> getAiTags(@PathVariable UUID customerId) {
        return customerTagService.getAiTags(customerId);
    }

    /**
     * 添加手动标签（客服手动添加）
     */
    @PostMapping("/manual")
    public CustomerDto addManualTag(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCustomerTagRequest request) {
        return customerTagService.addManualTag(customerId, request.tag());
    }

    /**
     * 删除手动标签（客服手动删除）
     */
    @DeleteMapping("/manual")
    public CustomerDto removeManualTag(
            @PathVariable UUID customerId,
            @Valid @RequestBody RemoveCustomerTagRequest request) {
        return customerTagService.removeManualTag(customerId, request.tag());
    }

    /**
     * 添加AI标签（AI自动添加）
     */
    @PostMapping("/ai")
    public CustomerDto addAiTag(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCustomerTagRequest request) {
        return customerTagService.addAiTag(customerId, request.tag());
    }

    /**
     * 删除AI标签（AI自动删除）
     */
    @DeleteMapping("/ai")
    public CustomerDto removeAiTag(
            @PathVariable UUID customerId,
            @Valid @RequestBody RemoveCustomerTagRequest request) {
        return customerTagService.removeAiTag(customerId, request.tag());
    }

    /**
     * 批量设置手动标签（覆盖现有手动标签）
     */
    @PutMapping("/manual")
    public CustomerDto setManualTags(
            @PathVariable UUID customerId,
            @RequestBody List<String> tags) {
        return customerTagService.setManualTags(customerId, tags);
    }

    /**
     * 批量设置AI标签（覆盖现有AI标签）
     */
    @PutMapping("/ai")
    public CustomerDto setAiTags(
            @PathVariable UUID customerId,
            @RequestBody List<String> tags) {
        return customerTagService.setAiTags(customerId, tags);
    }
}
