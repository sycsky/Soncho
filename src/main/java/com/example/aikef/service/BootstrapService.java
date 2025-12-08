package com.example.aikef.service;

import com.example.aikef.dto.BootstrapResponse;
import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.dto.SessionCategoryDto;
import com.example.aikef.dto.SessionGroupDto;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.SessionGroup;
import com.example.aikef.model.SessionGroupMapping;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.KnowledgeBaseRepository;
import com.example.aikef.repository.QuickReplyRepository;
import com.example.aikef.repository.RoleRepository;
import com.example.aikef.repository.SessionGroupMappingRepository;
import com.example.aikef.repository.SessionGroupRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BootstrapService {

    private final SessionGroupRepository sessionGroupRepository;
    private final SessionGroupMappingRepository sessionGroupMappingRepository;
    private final AgentRepository agentRepository;
    private final RoleRepository roleRepository;
    private final QuickReplyRepository quickReplyRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EntityMapper entityMapper;
    private final ReadRecordService readRecordService;
    private final SessionGroupService sessionGroupService;

    public BootstrapService(SessionGroupRepository sessionGroupRepository,
                            SessionGroupMappingRepository sessionGroupMappingRepository,
                            AgentRepository agentRepository,
                            RoleRepository roleRepository,
                            QuickReplyRepository quickReplyRepository,
                            KnowledgeBaseRepository knowledgeBaseRepository,
                            EntityMapper entityMapper,
                            ReadRecordService readRecordService,
                            SessionGroupService sessionGroupService) {
        this.sessionGroupRepository = sessionGroupRepository;
        this.sessionGroupMappingRepository = sessionGroupMappingRepository;
        this.agentRepository = agentRepository;
        this.roleRepository = roleRepository;
        this.quickReplyRepository = quickReplyRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.entityMapper = entityMapper;
        this.readRecordService = readRecordService;
        this.sessionGroupService = sessionGroupService;
    }

    public BootstrapResponse bootstrap(UUID agentId) {
        // 收集主要负责客服的会话ID和支持客服的会话ID
        List<UUID> primaryAgentSessionIds = new java.util.ArrayList<>();
        List<UUID> supportAgentSessionIds = new java.util.ArrayList<>();
        
        // 获取所有分组
        List<SessionGroup> groups = sessionGroupRepository.findByAgent_IdOrderBySortOrderAsc(agentId);
        
        List<SessionGroupDto> sessionGroups = groups.stream()
                .map(group -> {
                    // 获取分组下的会话
                    List<ChatSessionDto> sessions = sessionGroupMappingRepository
                            .findBySessionGroupIdAndAgentId(group.getId(), agentId)
                            .stream()
                            .map(SessionGroupMapping::getSession)
                            .map(session -> {
                                // 区分主要负责客服和支持客服的会话
                                if (session.getPrimaryAgent() != null && 
                                    session.getPrimaryAgent().getId().equals(agentId)) {
                                    primaryAgentSessionIds.add(session.getId());
                                } else {
                                    supportAgentSessionIds.add(session.getId());
                                }
                                return entityMapper.toChatSessionDtoForAgent(session, agentId);
                            })
                            .toList();
                    
                    // 获取分组绑定的分类列表（完整数据）
                    List<SessionCategoryDto> categories = sessionGroupService.getGroupBoundCategories(group.getId())
                            .stream()
                            .map(entityMapper::toSessionCategoryDto)
                            .toList();
                    
                    return entityMapper.toSessionGroupDtoWithSessionsAndCategories(group, sessions, categories);
                })
                .toList();

        // 批量查询主要负责客服的未读消息数
        java.util.Map<UUID, Integer> messageUnreadCountMap = primaryAgentSessionIds.isEmpty() 
                ? java.util.Collections.emptyMap()
                : readRecordService.getUnreadCountBatch(primaryAgentSessionIds, agentId);
        
        // 批量查询支持客服的未读@数量
        java.util.Map<UUID, Integer> mentionUnreadCountMap = supportAgentSessionIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : readRecordService.getMentionUnreadCountBatch(supportAgentSessionIds, agentId);
        
        // 更新未读数
        // - 主要负责客服：使用消息未读数
        // - 支持客服：使用 mention 未读数
        List<SessionGroupDto> updatedGroups = sessionGroups.stream()
                .map(group -> {
                    List<ChatSessionDto> updatedSessions = group.sessions().stream()
                            .map(session -> {
                                int unreadCount;
                                if (session.primaryAgentId() != null && 
                                    session.primaryAgentId().equals(agentId)) {
                                    // 主要负责客服：使用消息未读数
                                    unreadCount = messageUnreadCountMap.getOrDefault(session.id(), 0);
                                } else {
                                    // 支持客服：使用 mention 未读数
                                    unreadCount = mentionUnreadCountMap.getOrDefault(session.id(), 0);
                                }
                                return new ChatSessionDto(
                                    session.id(),
                                    session.userId(),
                                    session.user(),
                                    session.status(),
                                    session.lastActive(),
                                    unreadCount,
                                    session.sessionGroupId(),
                                    session.primaryAgentId(),
                                    session.agents(),
                                    session.lastMessage(),
                                    session.note(),
                                    session.categoryId(),
                                    session.category(),
                                    session.metadata(),
                                    session.customerLanguage()
                                );
                            })
                            .collect(java.util.stream.Collectors.toList());
                    return new SessionGroupDto(
                        group.id(),
                        group.name(),
                        group.system(),
                        group.agentId(),
                        group.icon(),
                        group.color(),
                        group.sortOrder(),
                        updatedSessions,
                        group.categories(),
                        group.createdAt(),
                        group.updatedAt()
                    );
                })
                .toList();

        return new BootstrapResponse(
                updatedGroups,
                agentRepository.findByIdWithRole(agentId).map(entityMapper::toAgentDto)
                        .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("坐席不存在")),
                roleRepository.findAll().stream().map(entityMapper::toRoleDto).toList(),
                quickReplyRepository.findAll().stream().map(entityMapper::toQuickReplyDto).toList(),
                knowledgeBaseRepository.findByEnabledTrue().stream().map(entityMapper::toKnowledgeEntryDto).toList());
    }
}
