package com.example.aikef.service;

import com.example.aikef.model.Agent;
import com.example.aikef.model.SessionCategory;
import com.example.aikef.model.SessionGroup;
import com.example.aikef.model.SessionGroupCategoryBinding;
import com.example.aikef.model.SessionGroupMapping;
import com.example.aikef.repository.SessionCategoryRepository;
import com.example.aikef.repository.SessionGroupCategoryBindingRepository;
import com.example.aikef.repository.SessionGroupMappingRepository;
import com.example.aikef.repository.SessionGroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Session 分组服务
 */
@Service
@Transactional
public class SessionGroupService {

    private static final Logger log = LoggerFactory.getLogger(SessionGroupService.class);
    
    private final SessionGroupRepository sessionGroupRepository;
    private final SessionGroupMappingRepository sessionGroupMappingRepository;
    private final SessionGroupCategoryBindingRepository categoryBindingRepository;
    private final SessionCategoryRepository sessionCategoryRepository;

    // 系统默认分组名称
    private static final String DEFAULT_GROUP_NAME = "Open";
    private static final String RESOLVED_GROUP_NAME = "Resolved";

    public SessionGroupService(SessionGroupRepository sessionGroupRepository,
                              SessionGroupMappingRepository sessionGroupMappingRepository,
                              SessionGroupCategoryBindingRepository categoryBindingRepository,
                              SessionCategoryRepository sessionCategoryRepository) {
        this.sessionGroupRepository = sessionGroupRepository;
        this.sessionGroupMappingRepository = sessionGroupMappingRepository;
        this.categoryBindingRepository = categoryBindingRepository;
        this.sessionCategoryRepository = sessionCategoryRepository;
    }

    /**
     * 确保客服有默认的系统分组
     */
    @Transactional
    public void ensureDefaultGroups(Agent agent) {
        log.info("🔍 检查客服默认分组: agentId={}, agentName={}", agent.getId(), agent.getName());
        
        List<SessionGroup> systemGroups = sessionGroupRepository.findByAgentAndSystemTrue(agent);
        log.debug("📋 当前系统分组数量: {}", systemGroups.size());
        
        // 检查是否有 Open 分组
        boolean hasOpenGroup = systemGroups.stream()
                .anyMatch(g -> DEFAULT_GROUP_NAME.equals(g.getName()));
        
        // 检查是否有 Resolved 分组
        boolean hasResolvedGroup = systemGroups.stream()
                .anyMatch(g -> RESOLVED_GROUP_NAME.equals(g.getName()));
        
        // 创建缺失的系统分组
        if (!hasOpenGroup) {
            log.info("✨ 创建默认分组 'Open' for agentId={}", agent.getId());
            createSystemGroup(agent, DEFAULT_GROUP_NAME, "📥", "#3B82F6", 0);
        } else {
            log.debug("✅ 'Open' 分组已存在");
        }
        
        if (!hasResolvedGroup) {
            log.info("✨ 创建默认分组 'Resolved' for agentId={}", agent.getId());
            createSystemGroup(agent, RESOLVED_GROUP_NAME, "✅", "#10B981", 999);
        } else {
            log.debug("✅ 'Resolved' 分组已存在");
        }
        
        log.info("✅ 默认分组检查完成: agentId={}", agent.getId());
    }

    /**
     * 创建系统分组
     */
    private SessionGroup createSystemGroup(Agent agent, String name, String icon, String color, int sortOrder) {
        SessionGroup group = new SessionGroup();
        group.setName(name);
        group.setSystem(true);
        group.setAgent(agent);
        group.setIcon(icon);
        group.setColor(color);
        group.setSortOrder(sortOrder);
        SessionGroup saved = sessionGroupRepository.save(group);
        log.info("💾 系统分组已保存: id={}, name={}, agentId={}", saved.getId(), name, agent.getId());
        return saved;
    }

    /**
     * 创建自定义分组
     */
    public SessionGroup createGroup(Agent agent, String name, String icon, String color) {
        // 检查名称是否已存在
        if (sessionGroupRepository.existsByAgentAndName(agent, name)) {
            throw new IllegalArgumentException("分组名称已存在: " + name);
        }
        
        // 系统分组名称不能被占用
        if (DEFAULT_GROUP_NAME.equals(name) || RESOLVED_GROUP_NAME.equals(name)) {
            throw new IllegalArgumentException("不能使用系统分组名称");
        }
        
        SessionGroup group = new SessionGroup();
        group.setName(name);
        group.setSystem(false);
        group.setAgent(agent);
        group.setIcon(icon);
        group.setColor(color);
        group.setSortOrder(100); // 自定义分组排在系统分组后面
        
        return sessionGroupRepository.save(group);
    }

    /**
     * 更新分组
     */
    public SessionGroup updateGroup(UUID groupId, String name, String icon, String color) {
        SessionGroup group = sessionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("分组不存在"));
        
        if (group.isSystem()) {
            throw new IllegalArgumentException("系统分组不能修改名称");
        }
        
        if (name != null && !name.equals(group.getName())) {
            // 检查新名称是否与其他分组冲突
            if (sessionGroupRepository.existsByAgentAndName(group.getAgent(), name)) {
                throw new IllegalArgumentException("分组名称已存在: " + name);
            }
            group.setName(name);
        }
        
        if (icon != null) {
            group.setIcon(icon);
        }
        
        if (color != null) {
            group.setColor(color);
        }
        
        return sessionGroupRepository.save(group);
    }

    /**
     * 删除分组，并将分组下的所有会话转移到默认分组
     * 
     * @param groupId 要删除的分组ID
     * @return 默认分组ID
     */
    public UUID deleteGroup(UUID groupId) {
        SessionGroup group = sessionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("分组不存在"));
        
        if (group.isSystem()) {
            throw new IllegalArgumentException("系统分组不能删除");
        }
        
        // 获取默认分组
        SessionGroup defaultGroup = getDefaultGroup(group.getAgent());
        
        // 查找该分组下的所有会话映射
        List<SessionGroupMapping> mappings = sessionGroupMappingRepository
                .findBySessionGroupIdAndAgentId(groupId, group.getAgent().getId());
        
        log.info("🔄 删除分组，转移 {} 个会话到默认分组: groupId={}, defaultGroupId={}", 
                mappings.size(), groupId, defaultGroup.getId());
        
        // 将所有会话转移到默认分组
        for (SessionGroupMapping mapping : mappings) {
            mapping.setSessionGroup(defaultGroup);
        }
        
        if (!mappings.isEmpty()) {
            sessionGroupMappingRepository.saveAll(mappings);
            log.info("✅ 已转移 {} 个会话到默认分组", mappings.size());
        }
        
        // 删除分组
        sessionGroupRepository.delete(group);
        log.info("🗑️ 分组已删除: groupId={}, name={}", groupId, group.getName());
        
        return defaultGroup.getId();
    }

    /**
     * 获取客服的所有分组
     */
    @Transactional(readOnly = true)
    public List<SessionGroup> getAgentGroups(UUID agentId) {
        return sessionGroupRepository.findByAgent_IdOrderBySortOrderAsc(agentId);
    }

    /**
     * 获取默认分组（Open）
     */
    @Transactional(readOnly = true)
    public SessionGroup getDefaultGroup(Agent agent) {
        return sessionGroupRepository.findByAgentAndName(agent, DEFAULT_GROUP_NAME)
                .orElseThrow(() -> new IllegalStateException("默认分组不存在，请联系管理员"));
    }

    /**
     * 获取已解决分组（Resolved）
     */
    @Transactional(readOnly = true)
    public SessionGroup getResolvedGroup(Agent agent) {
        return sessionGroupRepository.findByAgentAndName(agent, RESOLVED_GROUP_NAME)
                .orElseThrow(() -> new IllegalStateException("Resolved分组不存在，请联系管理员"));
    }
    
    /**
     * 根据ID获取分组
     */
    @Transactional(readOnly = true)
    public SessionGroup getGroupById(UUID groupId) {
        return sessionGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("分组不存在"));
    }

    // ==================== 分类绑定功能 ====================

    /**
     * 为分组绑定分类
     * 约束：同一Agent下，一个分类只能绑定到一个分组
     *
     * @param groupId 分组ID
     * @param categoryId 分类ID
     * @param agentId 客服ID
     */
    public void bindCategoryToGroup(UUID groupId, UUID categoryId, UUID agentId) {
        SessionGroup group = sessionGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("分组不存在"));

        // 校验：系统分组不能绑定分类
        if (group.isSystem()) {
            throw new IllegalArgumentException("系统分组不能绑定分类");
        }

        // 校验：分组必须属于当前客服
        if (!group.getAgent().getId().equals(agentId)) {
            throw new IllegalArgumentException("无权操作此分组");
        }

        SessionCategory category = sessionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        // 校验：该分类在该客服下是否已经被绑定
        if (categoryBindingRepository.existsByAgent_IdAndCategory_Id(agentId, categoryId)) {
            throw new IllegalArgumentException("该分类已被其他分组绑定，同一客服下每个分类只能绑定到一个分组");
        }

        // 创建绑定关系
        SessionGroupCategoryBinding binding = new SessionGroupCategoryBinding();
        binding.setSessionGroup(group);
        binding.setCategory(category);
        binding.setAgent(group.getAgent());

        categoryBindingRepository.save(binding);
        log.info("绑定分类到分组: groupId={}, categoryId={}, agentId={}", groupId, categoryId, agentId);
    }

    /**
     * 解除分组的分类绑定
     *
     * @param groupId 分组ID
     * @param categoryId 分类ID
     * @param agentId 客服ID
     */
    public void unbindCategoryFromGroup(UUID groupId, UUID categoryId, UUID agentId) {
        SessionGroup group = sessionGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("分组不存在"));

        // 校验：分组必须属于当前客服
        if (!group.getAgent().getId().equals(agentId)) {
            throw new IllegalArgumentException("无权操作此分组");
        }

        categoryBindingRepository.deleteBySessionGroup_IdAndCategory_Id(groupId, categoryId);
        log.info("解除分类绑定: groupId={}, categoryId={}, agentId={}", groupId, categoryId, agentId);
    }

    /**
     * 获取分组绑定的所有分类ID
     */
    @Transactional(readOnly = true)
    public List<UUID> getGroupBoundCategoryIds(UUID groupId) {
        return categoryBindingRepository.findBySessionGroup_Id(groupId)
                .stream()
                .map(binding -> binding.getCategory().getId())
                .collect(Collectors.toList());
    }

    /**
     * 获取分组绑定的所有分类（完整数据）
     */
    @Transactional(readOnly = true)
    public List<SessionCategory> getGroupBoundCategories(UUID groupId) {
        return categoryBindingRepository.findBySessionGroup_Id(groupId)
                .stream()
                .map(SessionGroupCategoryBinding::getCategory)
                .collect(Collectors.toList());
    }

    /**
     * 根据分类查找客服绑定的分组
     * 如果客服有分组绑定了该分类，返回该分组；否则返回默认分组
     *
     * @param agent 客服
     * @param categoryId 分类ID（可为null）
     * @return 匹配的分组或默认分组
     */
    @Transactional(readOnly = true)
    public SessionGroup findGroupByCategoryOrDefault(Agent agent, UUID categoryId) {
        if (categoryId != null) {
            // 查找该客服是否有分组绑定了该分类
            Optional<UUID> boundGroupId = categoryBindingRepository
                    .findGroupIdByAgentIdAndCategoryId(agent.getId(), categoryId);

            if (boundGroupId.isPresent()) {
                return sessionGroupRepository.findById(boundGroupId.get())
                        .orElseGet(() -> getDefaultGroup(agent));
            }
        }

        // 没有找到匹配的分组，返回默认分组
        return getDefaultGroup(agent);
    }

    /**
     * 检查某个客服下某个分类是否已被绑定
     */
    @Transactional(readOnly = true)
    public boolean isCategoryBoundByAgent(UUID agentId, UUID categoryId) {
        return categoryBindingRepository.existsByAgent_IdAndCategory_Id(agentId, categoryId);
    }

    /**
     * 获取客服所有的分类绑定关系
     */
    @Transactional(readOnly = true)
    public List<SessionGroupCategoryBinding> getAgentCategoryBindings(UUID agentId) {
        return categoryBindingRepository.findByAgent_Id(agentId);
    }

    /**
     * 获取客服可绑定的分类列表（排除已绑定的）
     * 
     * @param agentId 客服ID
     * @return 可绑定的分类列表
     */
    @Transactional(readOnly = true)
    public List<SessionCategory> getAvailableCategoriesForAgent(UUID agentId) {
        // 获取所有启用的分类
        List<SessionCategory> allEnabledCategories = sessionCategoryRepository.findByEnabledTrueOrderBySortOrderAsc();
        
        // 获取客服已绑定的分类ID
        List<UUID> boundCategoryIds = categoryBindingRepository.findByAgent_Id(agentId)
                .stream()
                .map(binding -> binding.getCategory().getId())
                .collect(Collectors.toList());
        
        // 过滤掉已绑定的分类
        return allEnabledCategories.stream()
                .filter(category -> !boundCategoryIds.contains(category.getId()))
                .collect(Collectors.toList());
    }
}
