package com.example.aikef.service;

import com.example.aikef.dto.SessionCategoryDto;
import com.example.aikef.dto.request.CreateSessionCategoryRequest;
import com.example.aikef.dto.request.UpdateSessionCategoryRequest;
import com.example.aikef.model.Agent;
import com.example.aikef.model.SessionCategory;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.SessionCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会话分类服务
 */
@Service
@Transactional
public class SessionCategoryService {

    private static final Logger log = LoggerFactory.getLogger(SessionCategoryService.class);

    private final SessionCategoryRepository sessionCategoryRepository;
    private final AgentRepository agentRepository;

    public SessionCategoryService(SessionCategoryRepository sessionCategoryRepository,
                                  AgentRepository agentRepository) {
        this.sessionCategoryRepository = sessionCategoryRepository;
        this.agentRepository = agentRepository;
    }

    /**
     * 创建分类
     */
    public SessionCategoryDto createCategory(CreateSessionCategoryRequest request, UUID createdByAgentId) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        // 检查名称是否已存在
        if (sessionCategoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("分类名称已存在: " + request.name());
        }

        Agent createdByAgent = agentRepository.findById(createdByAgentId)
                .orElseThrow(() -> new EntityNotFoundException("创建人不存在"));

        SessionCategory category = new SessionCategory();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setIcon(request.icon());
        category.setColor(request.color());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        category.setCreatedByAgent(createdByAgent);
        category.setEnabled(true);

        SessionCategory saved = sessionCategoryRepository.save(category);
        log.info("创建分类成功: id={}, name={}", saved.getId(), saved.getName());

        return toDto(saved);
    }

    /**
     * 更新分类
     */
    public SessionCategoryDto updateCategory(UUID categoryId, UpdateSessionCategoryRequest request) {
        SessionCategory category = sessionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        if (request.name() != null && !request.name().isBlank()) {
            // 检查新名称是否与其他分类冲突
            if (!request.name().equals(category.getName()) && 
                sessionCategoryRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("分类名称已存在: " + request.name());
            }
            category.setName(request.name());
        }

        if (request.description() != null) {
            category.setDescription(request.description());
        }

        if (request.icon() != null) {
            category.setIcon(request.icon());
        }

        if (request.color() != null) {
            category.setColor(request.color());
        }

        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }

        if (request.enabled() != null) {
            category.setEnabled(request.enabled());
        }

        SessionCategory updated = sessionCategoryRepository.save(category);
        log.info("更新分类成功: id={}, name={}", updated.getId(), updated.getName());

        return toDto(updated);
    }

    /**
     * 删除分类
     */
    public void deleteCategory(UUID categoryId) {
        SessionCategory category = sessionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        sessionCategoryRepository.delete(category);
        log.info("删除分类成功: id={}, name={}", categoryId, category.getName());
    }

    /**
     * 获取分类详情
     */
    @Transactional(readOnly = true)
    public SessionCategoryDto getCategory(UUID categoryId) {
        SessionCategory category = sessionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));
        return toDto(category);
    }

    /**
     * 获取分类实体
     */
    @Transactional(readOnly = true)
    public SessionCategory getCategoryEntity(UUID categoryId) {
        return sessionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));
    }

    /**
     * 获取所有启用的分类
     */
    @Transactional(readOnly = true)
    public List<SessionCategoryDto> getAllEnabledCategories() {
        return sessionCategoryRepository.findByEnabledTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有分类（包括禁用的）
     */
    @Transactional(readOnly = true)
    public List<SessionCategoryDto> getAllCategories() {
        return sessionCategoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID检查分类是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsById(UUID categoryId) {
        return sessionCategoryRepository.existsById(categoryId);
    }

    /**
     * 转换为DTO
     */
    private SessionCategoryDto toDto(SessionCategory category) {
        return new SessionCategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getColor(),
                category.getSortOrder(),
                category.isEnabled(),
                category.getCreatedByAgent() != null ? category.getCreatedByAgent().getId() : null,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}

