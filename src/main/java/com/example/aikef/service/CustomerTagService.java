package com.example.aikef.service;

import com.example.aikef.dto.CustomerDto;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 客户标签管理服务
 * 区分手动添加的标签(tags)和AI生成的标签(aiTags)
 */
@Service
@Transactional(readOnly = true)
public class CustomerTagService {

    private final CustomerRepository customerRepository;
    private final EntityMapper entityMapper;

    public CustomerTagService(CustomerRepository customerRepository, EntityMapper entityMapper) {
        this.customerRepository = customerRepository;
        this.entityMapper = entityMapper;
    }

    /**
     * 添加手动标签（客服手动添加）
     */
    @Transactional
    public CustomerDto addManualTag(UUID customerId, String tag) {
        Customer customer = findCustomerById(customerId);


        List<String> tags = new ArrayList<>(customer.getTags());
        if (!tags.contains(tag)) {
            tags.add(tag);
            customer.setTags(tags);
            customerRepository.save(customer);
        }
        
        return entityMapper.toCustomerDto(customer);
    }

    /**
     * 删除手动标签（客服手动删除）
     */
    @Transactional
    public CustomerDto removeManualTag(UUID customerId, String tag) {
        Customer customer = findCustomerById(customerId);
        
        List<String> tags = new ArrayList<>(customer.getTags());
        tags.remove(tag);
        customer.setTags(tags);
        customerRepository.save(customer);
        
        return entityMapper.toCustomerDto(customer);
    }

    /**
     * 添加AI标签（AI自动添加）
     */
    @Transactional
    public CustomerDto addAiTag(UUID customerId, String tag) {
        Customer customer = findCustomerById(customerId);
        
        List<String> aiTags = new ArrayList<>(customer.getAiTags());
        if (!aiTags.contains(tag)) {
            aiTags.add(tag);
            customer.setAiTags(aiTags);
            customerRepository.save(customer);
        }
        
        return entityMapper.toCustomerDto(customer);
    }

    /**
     * 删除AI标签（AI自动删除）
     */
    @Transactional
    public CustomerDto removeAiTag(UUID customerId, String tag) {
        Customer customer = findCustomerById(customerId);
        
        List<String> aiTags = new ArrayList<>(customer.getAiTags());
        aiTags.remove(tag);
        customer.setAiTags(aiTags);
        customerRepository.save(customer);
        
        return entityMapper.toCustomerDto(customer);
    }

    /**
     * 获取客户的所有手动标签
     */
    public List<String> getManualTags(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        return new ArrayList<>(customer.getTags());
    }

    /**
     * 获取客户的所有AI标签
     */
    public List<String> getAiTags(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        return new ArrayList<>(customer.getAiTags());
    }

    /**
     * 获取客户的所有标签（手动+AI）
     */
    public CustomerDto getAllTags(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        return entityMapper.toCustomerDto(customer);
    }

    /**
     * 批量设置手动标签（用于客服批量操作）
     */
    @Transactional
    public CustomerDto setManualTags(UUID customerId, List<String> tags) {
        Customer customer = findCustomerById(customerId);
        customer.setTags(new ArrayList<>(tags));
        customerRepository.save(customer);
        return entityMapper.toCustomerDto(customer);
    }

    /**
     * 批量设置AI标签（用于AI批量操作）
     */
    @Transactional
    public CustomerDto setAiTags(UUID customerId, List<String> tags) {
        Customer customer = findCustomerById(customerId);
        customer.setAiTags(new ArrayList<>(tags));
        customerRepository.save(customer);
        return entityMapper.toCustomerDto(customer);
    }

    private Customer findCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("客户不存在"));
    }
}
