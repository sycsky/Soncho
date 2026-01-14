package com.example.aikef.service;

import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.*;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.SessionCategoryRepository;
import com.example.aikef.repository.SessionGroupMappingRepository;
import com.example.aikef.repository.SessionGroupRepository;
import com.example.aikef.service.strategy.AgentAssignmentStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天会话服务
 */
@Service
@Transactional(readOnly = true)
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final AgentAssignmentStrategy agentAssignmentStrategy;
    private final SessionGroupMappingRepository sessionGroupMappingRepository;
    private final SessionGroupRepository sessionGroupRepository;
    private final SessionGroupService sessionGroupService;
    private final AgentRepository agentRepository;
    private final EntityMapper entityMapper;
    private final ReadRecordService readRecordService;
    private final SessionCategoryRepository sessionCategoryRepository;
    private final ObjectMapper objectMapper;

    public ChatSessionService(ChatSessionRepository chatSessionRepository,
                             AgentAssignmentStrategy agentAssignmentStrategy,
                             SessionGroupMappingRepository sessionGroupMappingRepository,
                             SessionGroupRepository sessionGroupRepository,
                             SessionGroupService sessionGroupService,
                             AgentRepository agentRepository,
                             EntityMapper entityMapper,
                             @Lazy ReadRecordService readRecordService,
                             SessionCategoryRepository sessionCategoryRepository,
                             ObjectMapper objectMapper) {
        this.chatSessionRepository = chatSessionRepository;
        this.agentAssignmentStrategy = agentAssignmentStrategy;
        this.sessionGroupMappingRepository = sessionGroupMappingRepository;
        this.sessionGroupRepository = sessionGroupRepository;
        this.sessionGroupService = sessionGroupService;
        this.agentRepository = agentRepository;
        this.entityMapper = entityMapper;
        this.readRecordService = readRecordService;
        this.sessionCategoryRepository = sessionCategoryRepository;
        this.objectMapper = objectMapper;
    }

    /** 为客户创建聊天会话并分配客服（无元数据） */
    @Transactional
    public ChatSession createSessionForCustomer(Customer customer) {
        return createSessionForCustomer(customer, null);
    }

    /** 
     * 为客户创建聊天会话并分配客服（带元数据）
     * 
     * @param customer 客户
     * @param metadata 会话元数据，可包含：
     *                 - categoryId: 会话分类ID (String，UUID格式)
     *                 - language: 客户使用的语言代码（如 zh-TW, en, ja）
     *                 - source: 来源渠道
     *                 - referrer: 来源页面
     *                 - device: 设备信息
     *                 - 其他自定义字段...
     */
    @Transactional
    public ChatSession createSessionForCustomer(Customer customer, Map<String, Object> metadata) {
        // 从 metadata 中提取 categoryId
        UUID categoryId = extractCategoryId(metadata);
        // 从 metadata 中提取 language
        String customerLanguage = extractLanguage(metadata);
        
        log.info("为客户 {} 创建聊天会话, categoryId={}, language={}, metadata={}", 
                customer.getName(), categoryId, customerLanguage, metadata);
        
        // 分配主责客服
        Agent primaryAgent = agentAssignmentStrategy.assignPrimaryAgent(
                customer,
                customer.getPrimaryChannel()
        );

        // 分配支持客服（可选）
        List<Agent> supportAgents = agentAssignmentStrategy.assignSupportAgents(
                customer,
                customer.getPrimaryChannel(),
                primaryAgent
        );

        // 创建会话
        ChatSession session = new ChatSession();
        session.setCustomer(customer);
        session.setPrimaryAgent(primaryAgent);
        session.setStatus(SessionStatus.AI_HANDLING);
        session.setLastActiveAt(Instant.now());

        // 设置客户语言（如果提供了）
        if (customerLanguage != null && !customerLanguage.isBlank()) {
            session.setCustomerLanguage(customerLanguage);
            log.info("会话设置客户语言: language={}", customerLanguage);
        }

        // 设置分类（如果提供了）
        if (categoryId != null) {
            SessionCategory category = sessionCategoryRepository.findById(categoryId)
                    .orElse(null);
            if (category != null && category.isEnabled()) {
                session.setCategory(category);
                log.info("会话设置分类: categoryId={}, categoryName={}", categoryId, category.getName());
            } else {
                log.warn("分类不存在或已禁用: categoryId={}", categoryId);
            }
        }

        // 设置元数据（JSON 格式存储）
        if (metadata != null && !metadata.isEmpty()) {
            try {
                session.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("序列化 metadata 失败: {}", e.getMessage());
            }
        }

        // 添加支持客服 ID
        if (!supportAgents.isEmpty()) {
            List<UUID> supportAgentIds = supportAgents.stream()
                    .map(Agent::getId)
                    .toList();
            session.setSupportAgentIds(new ArrayList<>(supportAgentIds));
        }

        ChatSession savedSession = chatSessionRepository.save(session);
        log.info("创建会话成功: SessionID={}, 主责客服={}, 支持客服数={}, categoryId={}, language={}",
                savedSession.getId(),
                primaryAgent.getName(),
                supportAgents.size(),
                categoryId,
                customerLanguage);

        // 将会话分配到客服的分组（根据分类自动匹配或使用默认分组）
        assignSessionToAgentGroup(savedSession, primaryAgent, categoryId);
        for (Agent agent : supportAgents) {
            assignSessionToAgentGroup(savedSession, agent, categoryId);
        }

        return savedSession;
    }

    /**
     * 从 metadata 中提取 categoryId
     */
    private UUID extractCategoryId(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        
        Object categoryIdObj = metadata.get("categoryId");
        if (categoryIdObj == null) {
            return null;
        }
        
        try {
            if (categoryIdObj instanceof String str) {
                return UUID.fromString(str);
            } else if (categoryIdObj instanceof UUID uuid) {
                return uuid;
            }
        } catch (IllegalArgumentException e) {
            log.warn("无效的 categoryId 格式: {}", categoryIdObj);
        }
        
        return null;
    }

    /**
     * 从 metadata 中提取 language（客户使用的语言代码）
     */
    private String extractLanguage(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        
        Object languageObj = metadata.get("language");
        if (languageObj == null) {
            return null;
        }
        
        if (languageObj instanceof String str && !str.isBlank()) {
            return str;
        }
        
        return null;
    }

    /**
     * 根据分类自动分配会话到客服的分组
     * 如果客服有分组绑定了该分类，则分配到该分组；否则分配到默认分组
     */
    public void assignSessionToAgentGroup(ChatSession session, Agent agent, UUID categoryId) {
        if (session == null || agent == null) {
            return;
        }

        // 确保客服具备默认系统分组（Open / Resolved）
        sessionGroupService.ensureDefaultGroups(agent);
        
        // 根据分类查找匹配的分组，如果没有则返回默认分组
        SessionGroup targetGroup = sessionGroupService.findGroupByCategoryOrDefault(agent, categoryId);

        SessionGroupMapping mapping = sessionGroupMappingRepository
                .findBySessionAndAgent(session, agent)
                .orElse(null);
        if (mapping == null) {
            mapping = new SessionGroupMapping();
            mapping.setSession(session);
            mapping.setAgent(agent);
        }

        mapping.setSessionGroup(targetGroup);
        sessionGroupMappingRepository.save(mapping);
        
        if (categoryId != null && !targetGroup.isSystem()) {
            log.info("根据分类分配会话到分组: sessionId={}, agentId={}, groupId={}, categoryId={}",
                    session.getId(), agent.getId(), targetGroup.getId(), categoryId);
        } else {
            log.info("分配会话到默认分组: sessionId={}, agentId={}, groupId={}",
                    session.getId(), agent.getId(), targetGroup.getId());
        }
    }

    /**
     * @deprecated 使用 assignSessionToAgentGroup 代替
     */
    @Deprecated
    private void assignSessionToAgentDefaultGroup(ChatSession session, Agent agent) {
        assignSessionToAgentGroup(session, agent, null);
    }

    /**
     * 获取会话详情
     */
    public ChatSession getSession(UUID sessionId) {
        ChatSession chatSession = chatSessionRepository.getReferenceById(sessionId);
        Hibernate.initialize(chatSession.getCustomer());
        return chatSession;
    }

    /**
     * 获取会话详情并转换为DTO（在事务内完成）
     * 
     * @param sessionId 会话ID
     * @param agentId 客服ID
     * @return 会话DTO（只包含该客服视角下的分组ID）
     */
    public ChatSessionDto getSessionDto(UUID sessionId, UUID agentId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        ChatSessionDto dto = entityMapper.toChatSessionDtoForAgent(session, agentId);
        
        // 计算未读数：
        // - 主要负责客服：使用消息未读数
        // - 支持客服：使用 mention 未读数（创建时间 > 最后阅读时间的@数量）
        int unreadCount = 0;
        if (dto.primaryAgentId() != null && dto.primaryAgentId().equals(agentId)) {
            // 主要负责客服：统计未读消息数
            unreadCount = readRecordService.getUnreadCount(sessionId, agentId);
        } else {
            // 支持客服：统计未读@数量
            unreadCount = readRecordService.getMentionUnreadCount(sessionId, agentId);
        }
        
        // 返回带未读数的DTO
        return new ChatSessionDto(
            dto.id(),
            dto.userId(),
            dto.user(),
            dto.status(),
            dto.lastActive(),
            unreadCount,
            dto.sessionGroupId(),
            dto.primaryAgentId(),
            dto.agents(),
            dto.lastMessage(),
            dto.note(),
            dto.categoryId(),
            dto.category(),
            dto.metadata(),
            dto.customerLanguage()
        );
    }

    /**
     * 更新会话最后活跃时间
     */
    @Transactional
    public void updateLastActiveTime(UUID sessionId) {
        ChatSession session = getSession(sessionId);
        session.setLastActiveAt(Instant.now());
        chatSessionRepository.save(session);
    }

    /**
     * 检查用户是否是会话成员
     * 增加对管理员角色的支持：如果是管理员，默认有权访问所有会话
     */
    public boolean isSessionMember(UUID sessionId, UUID agentId, UUID customerId) {
        ChatSession session = getSession(sessionId);

        // 检查是否是客户
        if (customerId != null && session.getCustomer() != null
                && session.getCustomer().getId().equals(customerId)) {
            return true;
        }

        if (agentId != null) {
            // 1. 检查是否是主责客服
            if (session.getPrimaryAgent() != null
                    && session.getPrimaryAgent().getId().equals(agentId)) {
                return true;
            }

            // 2. 检查是否是支持客服
            if (session.getSupportAgentIds() != null && session.getSupportAgentIds().contains(agentId)) {
                return true;
            }
            
            // 3. 检查是否是管理员 (从数据库加载 Agent 并检查角色)
            // 注意：频繁查询可能影响性能，最好从 SecurityContext 中获取角色信息
            // 但这里 service 层通常不直接依赖 SecurityContext
            // 我们可以简单查询一下 Agent 的 Role
            return agentRepository.findByIdWithRole(agentId)
                    .map(agent -> agent.getRole() != null && "Administrator".equalsIgnoreCase(agent.getRole().getName()))
                    .orElse(false);
        }

        return false;
    }

    /**
     * 结束会话
     * 1. 将会话状态设置为 RESOLVED
     * 2. 将会话移动到所有参与客服的 Resolved 分组
     */
    @Transactional
    public void closeSession(UUID sessionId) {
        ChatSession session = getSession(sessionId);
        session.setStatus(SessionStatus.RESOLVED);
        chatSessionRepository.save(session);
        
        // 将会话移动到所有参与客服的 Resolved 分组
        moveSessionToResolvedGroupForAllAgents(session);
        
        log.info("会话已关闭: {}", sessionId);
    }
    
    /**
     * 将会话移动到所有参与客服的 Resolved 分组
     * 包括主责客服和所有支持客服
     */
    private void moveSessionToResolvedGroupForAllAgents(ChatSession session) {
        List<UUID> allAgentIds = new ArrayList<>();
        
        // 添加主责客服
        if (session.getPrimaryAgent() != null) {
            allAgentIds.add(session.getPrimaryAgent().getId());
        }
        
        // 添加所有支持客服
        if (session.getSupportAgentIds() != null && !session.getSupportAgentIds().isEmpty()) {
            allAgentIds.addAll(session.getSupportAgentIds());
        }
        
        if (allAgentIds.isEmpty()) {
            log.debug("会话 {} 没有参与客服，跳过移动到 Resolved 分组", session.getId());
            return;
        }
        
        log.info("将会话移动到 Resolved 分组: sessionId={}, agentCount={}", session.getId(), allAgentIds.size());
        
        for (UUID agentId : allAgentIds) {
            try {
                Agent agent = agentRepository.findById(agentId).orElse(null);
                if (agent == null) {
                    log.warn("客服不存在，跳过: agentId={}", agentId);
                    continue;
                }
                
                // 获取该客服的 Resolved 分组
                SessionGroup resolvedGroup = sessionGroupService.getResolvedGroup(agent);
                
                // 查找或创建映射
                SessionGroupMapping mapping = sessionGroupMappingRepository
                        .findBySessionIdAndAgentId(session.getId(), agentId)
                        .orElse(null);
                
                if (mapping == null) {
                    // 创建新映射
                    mapping = new SessionGroupMapping();
                    mapping.setSession(session);
                    mapping.setAgent(agent);
                }
                
                mapping.setSessionGroup(resolvedGroup);
                sessionGroupMappingRepository.save(mapping);
                
                log.debug("会话已移动到 Resolved 分组: sessionId={}, agentId={}, groupId={}", 
                        session.getId(), agentId, resolvedGroup.getId());
                        
            } catch (Exception e) {
                log.error("移动会话到 Resolved 分组失败: sessionId={}, agentId={}, error={}", 
                        session.getId(), agentId, e.getMessage());
            }
        }
    }
    
    /**
     * 根据ID查找会话
     */
    public ChatSession findById(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
    }

    /**
     * 更新会话备注
     */
    @Transactional
    public String updateSessionNote(UUID sessionId, String note) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        session.setNote(note);
        chatSessionRepository.save(session);
        return session.getNote();
    }

    /**
     * 获取会话备注
     */
    public String getSessionNote(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        return session.getNote();
    }

    /**
     * 将会话移动到指定客服的分组
     * 
     * @param sessionId 会话ID
     * @param agentId 客服ID
     * @param groupId 目标分组ID（可以为null表示移出分组）
     */
    @Transactional
    public void moveSessionToGroup(UUID sessionId, UUID agentId, UUID groupId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("客服不存在"));
        
        // 查找或创建映射
        SessionGroupMapping mapping = sessionGroupMappingRepository
                .findBySessionIdAndAgentId(sessionId, agentId)
                .orElse(null);
        
        if (groupId == null) {
            // 移出分组 - 删除映射
            if (mapping != null) {
                sessionGroupMappingRepository.delete(mapping);
                log.info("移出分组: sessionId={}, agentId={}", sessionId, agentId);
            }
        } else {
            // 移动到新分组
            SessionGroup targetGroup = sessionGroupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException("分组不存在"));
            
            if (mapping == null) {
                // 创建新映射
                mapping = new SessionGroupMapping();
                mapping.setSession(session);
                mapping.setAgent(agent);
            }
            
            mapping.setSessionGroup(targetGroup);
            sessionGroupMappingRepository.save(mapping);
            log.info("移动会话到分组: sessionId={}, agentId={}, groupId={}", 
                    sessionId, agentId, groupId);
        }
    }

    // ==================== 支持客服管理 ====================

    /**
     * 获取会话的所有客服（主要客服 + 支持客服）
     * 
     * @param sessionId 会话ID
     * @return 客服列表（主要客服在前）
     */
    public List<Agent> getSessionAgents(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        List<Agent> agents = new ArrayList<>();
        
        // 添加主要客服
        if (session.getPrimaryAgent() != null) {
            agentRepository.findByIdWithRole(session.getPrimaryAgent().getId())
                    .ifPresent(agents::add);
        }
        
        // 添加支持客服
        if (session.getSupportAgentIds() != null && !session.getSupportAgentIds().isEmpty()) {
            for (UUID supportAgentId : session.getSupportAgentIds()) {
                agentRepository.findByIdWithRole(supportAgentId)
                        .ifPresent(agents::add);
            }
        }
        
        return agents;
    }

    /**
     * 获取会话中的其他客服（排除指定客服）
     * 包含主要客服和支持客服
     * 
     * @param sessionId 会话ID
     * @param excludeAgentId 要排除的客服ID（通常是当前登录的客服）
     * @return 其他客服列表
     */
    public List<Agent> getOtherSessionAgents(UUID sessionId, UUID excludeAgentId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        List<Agent> agents = new ArrayList<>();
        
        // 添加主要客服（如果不是被排除的客服）
        if (session.getPrimaryAgent() != null && 
            !session.getPrimaryAgent().getId().equals(excludeAgentId)) {
            agentRepository.findByIdWithRole(session.getPrimaryAgent().getId())
                    .ifPresent(agents::add);
        }
        
        // 添加支持客服（排除指定客服）
        if (session.getSupportAgentIds() != null && !session.getSupportAgentIds().isEmpty()) {
            for (UUID supportAgentId : session.getSupportAgentIds()) {
                if (!supportAgentId.equals(excludeAgentId)) {
                    agentRepository.findByIdWithRole(supportAgentId)
                            .ifPresent(agents::add);
                }
            }
        }
        
        return agents;
    }

    /**
     * 获取可分配给会话的客服列表
     * 排除当前会话的主要客服和已有的支持客服
     * 
     * @param sessionId 会话ID
     * @return 可分配的客服列表
     */
    public List<Agent> getAvailableAgentsForSession(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        // 获取所有客服
        List<Agent> allAgents = agentRepository.findAll();
        
        // 获取需要排除的客服ID
        List<UUID> excludeIds = new ArrayList<>();
        
        // 排除主要客服
        if (session.getPrimaryAgent() != null) {
            excludeIds.add(session.getPrimaryAgent().getId());
        }
        
        // 排除已有的支持客服
        if (session.getSupportAgentIds() != null) {
            excludeIds.addAll(session.getSupportAgentIds());
        }



        // 过滤并返回可分配的客服
        return allAgents.stream()
                .filter(agent -> {

                    if(!excludeIds.contains(agent.getId())){
                        Hibernate.initialize(agent.getRole());
                    }

                    return !excludeIds.contains(agent.getId());
                })
                .toList();
    }

    /**
     * 为会话分配支持客服
     * 
     * @param sessionId 会话ID
     * @param agentId 要分配的客服ID
     */
    @Transactional
    public void assignSupportAgent(UUID sessionId, UUID agentId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("客服不存在"));
        
        // 检查是否是主要客服
        if (session.getPrimaryAgent() != null && session.getPrimaryAgent().getId().equals(agentId)) {
            throw new IllegalArgumentException("不能将主要客服添加为支持客服");
        }
        
        // 检查是否已经是支持客服
        List<UUID> supportAgentIds = session.getSupportAgentIds();
        if (supportAgentIds == null) {
            supportAgentIds = new ArrayList<>();
        }
        
        if (supportAgentIds.contains(agentId)) {
            throw new IllegalArgumentException("该客服已经是支持客服");
        }
        
        // 添加支持客服
        supportAgentIds.add(agentId);
        session.setSupportAgentIds(new ArrayList<>(supportAgentIds));
        chatSessionRepository.save(session);
        
        // 将会话分配到该客服的默认分组
        UUID categoryId = session.getCategory() != null ? session.getCategory().getId() : null;
        assignSessionToAgentGroup(session, agent, categoryId);
        
        log.info("分配支持客服: sessionId={}, agentId={}", sessionId, agentId);
    }

    /**
     * 移除会话的支持客服
     * 
     * @param sessionId 会话ID
     * @param agentId 要移除的客服ID
     */
    @Transactional
    public void removeSupportAgent(UUID sessionId, UUID agentId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        List<UUID> supportAgentIds = session.getSupportAgentIds();
        if (supportAgentIds == null || !supportAgentIds.contains(agentId)) {
            throw new IllegalArgumentException("该客服不是当前会话的支持客服");
        }
        
        // 移除支持客服
        supportAgentIds.remove(agentId);
        session.setSupportAgentIds(new ArrayList<>(supportAgentIds));
        chatSessionRepository.save(session);
        
        // 移除该客服对该会话的分组映射
        sessionGroupMappingRepository.findBySessionIdAndAgentId(sessionId, agentId)
                .ifPresent(sessionGroupMappingRepository::delete);
        
        log.info("移除支持客服: sessionId={}, agentId={}", sessionId, agentId);
    }

    // ==================== 转移会话功能 ====================

    /**
     * 获取可转移的客服列表
     * 排除当前会话的主要负责客服
     * 
     * @param sessionId 会话ID
     * @param currentAgentId 当前操作的客服ID（必须是主要负责客服）
     * @return 可转移的客服列表
     */
    public List<Agent> getTransferableAgents(UUID sessionId, UUID currentAgentId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        // 验证当前操作者是否是主要负责客服
        if (session.getPrimaryAgent() == null || 
            !session.getPrimaryAgent().getId().equals(currentAgentId)) {
            throw new IllegalArgumentException("只有主要负责客服可以转移会话");
        }
        
        // 获取所有客服，排除当前主要负责客服
        List<Agent> allAgents = agentRepository.findAll();
        
        return allAgents.stream()
                .filter(agent -> {

                    if(!agent.getId().equals(currentAgentId)){
                        Hibernate.initialize(agent.getRole());
                    }

                    return !agent.getId().equals(currentAgentId);
                })
                .toList();
    }

    /**
     * 转移会话到新的主要负责客服
     * 
     * @param sessionId 会话ID
     * @param currentAgentId 当前操作的客服ID（必须是主要负责客服）
     * @param newPrimaryAgentId 新的主要负责客服ID
     */
    @Transactional
    public void transferSession(UUID sessionId, UUID currentAgentId, UUID newPrimaryAgentId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        // 验证当前操作者是否是主要负责客服
        if (session.getPrimaryAgent() == null || 
            !session.getPrimaryAgent().getId().equals(currentAgentId)) {
            throw new IllegalArgumentException("只有主要负责客服可以转移会话");
        }
        
        // 验证新客服是否存在
        Agent newPrimaryAgent = agentRepository.findById(newPrimaryAgentId)
                .orElseThrow(() -> new EntityNotFoundException("目标客服不存在"));
        
        // 不能转移给自己
        if (currentAgentId.equals(newPrimaryAgentId)) {
            throw new IllegalArgumentException("不能将会话转移给自己");
        }
        
        UUID oldPrimaryAgentId = session.getPrimaryAgent().getId();
        
        // 如果新的主要负责客服原本是支持客服，从支持客服列表中移除
        List<UUID> supportAgentIds = session.getSupportAgentIds();
        if (supportAgentIds != null && supportAgentIds.contains(newPrimaryAgentId)) {
            supportAgentIds.remove(newPrimaryAgentId);
            session.setSupportAgentIds(new ArrayList<>(supportAgentIds));
        }
        
        // 设置新的主要负责客服
        session.setPrimaryAgent(newPrimaryAgent);
        session.setStatus(SessionStatus.HUMAN_HANDLING);
        chatSessionRepository.save(session);
        
        // 确保新客服有默认分组
        sessionGroupService.ensureDefaultGroups(newPrimaryAgent);
        
        // 为新的主要负责客服分配会话到分组
        UUID categoryId = session.getCategory() != null ? session.getCategory().getId() : null;
        assignSessionToAgentGroup(session, newPrimaryAgent, categoryId);
        
        // 移除原主要负责客服的分组映射（可选：如果希望原客服保留会话可以注释掉）
        sessionGroupMappingRepository.findBySessionIdAndAgentId(sessionId, oldPrimaryAgentId)
                .ifPresent(sessionGroupMappingRepository::delete);
        
        log.info("转移会话: sessionId={}, fromAgentId={}, toAgentId={}", 
                sessionId, oldPrimaryAgentId, newPrimaryAgentId);
    }

    /**
     * 转移会话并保留原客服为支持客服
     * 
     * @param sessionId 会话ID
     * @param currentAgentId 当前操作的客服ID（必须是主要负责客服）
     * @param newPrimaryAgentId 新的主要负责客服ID
     * @param keepAsSupport 是否将原主要客服保留为支持客服
     */
    @Transactional
    public void transferSession(UUID sessionId, UUID currentAgentId, UUID newPrimaryAgentId, boolean keepAsSupport) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));
        
        // 验证当前操作者是否是主要负责客服
        if (session.getPrimaryAgent() == null || 
            !session.getPrimaryAgent().getId().equals(currentAgentId)) {
            throw new IllegalArgumentException("只有主要负责客服可以转移会话");
        }
        
        // 验证新客服是否存在
        Agent newPrimaryAgent = agentRepository.findById(newPrimaryAgentId)
                .orElseThrow(() -> new EntityNotFoundException("目标客服不存在"));
        
        // 不能转移给自己
        if (currentAgentId.equals(newPrimaryAgentId)) {
            throw new IllegalArgumentException("不能将会话转移给自己");
        }
        
        UUID oldPrimaryAgentId = session.getPrimaryAgent().getId();
        Agent oldPrimaryAgent = session.getPrimaryAgent();
        
        // 如果新的主要负责客服原本是支持客服，从支持客服列表中移除
        List<UUID> supportAgentIds = session.getSupportAgentIds() != null 
                ? new ArrayList<>(session.getSupportAgentIds()) 
                : new ArrayList<>();
        supportAgentIds.remove(newPrimaryAgentId);
        
        // 如果需要保留原客服为支持客服
        if (keepAsSupport && !supportAgentIds.contains(oldPrimaryAgentId)) {
            supportAgentIds.add(oldPrimaryAgentId);
        }
        
        session.setSupportAgentIds(supportAgentIds);
        
        // 设置新的主要负责客服
        session.setPrimaryAgent(newPrimaryAgent);
        session.setStatus(SessionStatus.HUMAN_HANDLING);
        chatSessionRepository.save(session);
        
        // 确保新客服有默认分组
        sessionGroupService.ensureDefaultGroups(newPrimaryAgent);
        
        // 为新的主要负责客服分配会话到分组
        UUID categoryId = session.getCategory() != null ? session.getCategory().getId() : null;
        assignSessionToAgentGroup(session, newPrimaryAgent, categoryId);
        
        // 如果不保留原客服，移除其分组映射
        if (!keepAsSupport) {
            sessionGroupMappingRepository.findBySessionIdAndAgentId(sessionId, oldPrimaryAgentId)
                    .ifPresent(sessionGroupMappingRepository::delete);
        }
        
        log.info("转移会话: sessionId={}, fromAgentId={}, toAgentId={}, keepAsSupport={}", 
                sessionId, oldPrimaryAgentId, newPrimaryAgentId, keepAsSupport);
    }

    /**
     * 检查会话是否已解决，如果是，则重新打开并移动到 Open 分组
     */
    @Transactional
    public void checkAndReopenResolvedSession(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));

        if (session.getStatus() == SessionStatus.RESOLVED) {
            log.info("重新打开已解决的会话: {}", sessionId);
            
            // 1. 更新状态为 AI_HANDLING (作为默认的 Open 状态)
            session.setStatus(SessionStatus.AI_HANDLING);
            session.setLastActiveAt(Instant.now());
            chatSessionRepository.save(session);

            // 2. 移动到 Open 分组
            // 为主责客服移动
            if (session.getPrimaryAgent() != null) {
                UUID categoryId = session.getCategory() != null ? session.getCategory().getId() : null;
                assignSessionToAgentGroup(session, session.getPrimaryAgent(), categoryId);
            }
            
            // 为所有支持客服移动
            if (session.getSupportAgentIds() != null) {
                for (UUID supportAgentId : session.getSupportAgentIds()) {
                    agentRepository.findById(supportAgentId).ifPresent(agent -> {
                        UUID categoryId = session.getCategory() != null ? session.getCategory().getId() : null;
                        assignSessionToAgentGroup(session, agent, categoryId);
                    });
                }
            }
            
            log.info("会话 {} 已重新打开并移动到默认分组", sessionId);
        }
    }
}
