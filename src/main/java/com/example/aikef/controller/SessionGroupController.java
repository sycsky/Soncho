package com.example.aikef.controller;

import com.example.aikef.dto.ChatSessionDto;
import com.example.aikef.dto.SessionCategoryDto;
import com.example.aikef.dto.SessionGroupDto;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.*;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.SessionGroupMappingRepository;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.ChatSessionService;
import com.example.aikef.service.SessionCategoryService;
import com.example.aikef.service.SessionGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Session 分组管理接口
 */
@RestController
@RequestMapping("/api/v1/session-groups")
public class SessionGroupController {

    private final SessionGroupService sessionGroupService;
    private final AgentRepository agentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SessionGroupMappingRepository sessionGroupMappingRepository;
    private final ChatSessionService chatSessionService;
    private final EntityMapper entityMapper;
    private final SessionCategoryService sessionCategoryService;

    public SessionGroupController(SessionGroupService sessionGroupService,
                                 AgentRepository agentRepository,
                                 ChatSessionRepository chatSessionRepository,
                                 SessionGroupMappingRepository sessionGroupMappingRepository,
                                 ChatSessionService chatSessionService,
                                 EntityMapper entityMapper,
                                 SessionCategoryService sessionCategoryService) {
        this.sessionGroupService = sessionGroupService;
        this.agentRepository = agentRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.sessionGroupMappingRepository = sessionGroupMappingRepository;
        this.chatSessionService = chatSessionService;
        this.entityMapper = entityMapper;
        this.sessionCategoryService = sessionCategoryService;
    }

    /**
     * 获取当前客服的所有分组（带sessions和绑定的分类）
     */
    @GetMapping
    public List<SessionGroupDto> getMyGroups(Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        
        // 查询客服的所有分组
        List<SessionGroup> sessionGroups = sessionGroupService.getAgentGroups(agent.getId());
        
        // 为每个分组填充sessions和绑定的分类
        List<SessionGroupDto> result = new ArrayList<>();
        for (SessionGroup group : sessionGroups) {
            // 查询该分组下的所有映射
            List<SessionGroupMapping> mappings = sessionGroupMappingRepository
                    .findBySessionGroupIdAndAgentId(group.getId(), agent.getId());
            
            // 转换为ChatSessionDto（传递agentId）
            List<ChatSessionDto> groupSessions = mappings.stream()
                    .map(SessionGroupMapping::getSession)
                    .map(session -> entityMapper.toChatSessionDtoForAgent(session, agent.getId()))
                    .collect(Collectors.toList());
            
            // 获取分组绑定的分类列表（完整数据）
            List<SessionCategoryDto> categories = sessionGroupService.getGroupBoundCategories(group.getId())
                    .stream()
                    .map(category -> new SessionCategoryDto(
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
                    ))
                    .collect(Collectors.toList());
            
            result.add(entityMapper.toSessionGroupDtoWithSessionsAndCategories(group, groupSessions, categories));
        }
        
        return result;
    }

    /**
     * 创建分组
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionGroupDto createGroup(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        
        String name = request.get("name");
        String icon = request.get("icon");
        String color = request.get("color");
        
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        
        SessionGroup group = sessionGroupService.createGroup(agent, name, icon, color);
        return entityMapper.toSessionGroupDto(group);
    }

    /**
     * 更新分组
     */
    @PutMapping("/{id}")
    public SessionGroupDto updateGroup(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        // 验证分组所有权
        getCurrentAgent(authentication);
        
        String name = request.get("name");
        String icon = request.get("icon");
        String color = request.get("color");
        
        SessionGroup group = sessionGroupService.updateGroup(id, name, icon, color);
        return entityMapper.toSessionGroupDto(group);
    }

    /**
     * 删除分组
     * 删除分组时，会将该分组下的所有会话转移到默认分组
     * 
     * @return 返回默认分组ID（会话被转移到的目标分组）
     */
    @DeleteMapping("/{id}")
    public Map<String, UUID> deleteGroup(@PathVariable UUID id, Authentication authentication) {
        // 验证分组所有权
        getCurrentAgent(authentication);
        
        UUID defaultGroupId = sessionGroupService.deleteGroup(id);
        
        return Map.of("defaultGroupId", defaultGroupId);
    }
    
    /**
     * 移动会话到指定分组
     * POST /api/v1/session-groups/{groupId}/sessions/{sessionId}
     */
    @PostMapping("/{groupId}/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveSessionToGroup(
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId,
            Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        chatSessionService.moveSessionToGroup(sessionId, agent.getId(), groupId);
    }
    
    /**
     * 移出分组（设置为null）
     * DELETE /api/v1/session-groups/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSessionFromGroup(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        chatSessionService.moveSessionToGroup(sessionId, agent.getId(), null);
    }

    // ==================== 分类绑定功能 ====================

    /**
     * 为分组绑定分类
     * POST /api/v1/session-groups/{groupId}/categories/{categoryId}
     * 约束：同一客服下，每个分类只能绑定到一个分组
     */
    @PostMapping("/{groupId}/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bindCategoryToGroup(
            @PathVariable UUID groupId,
            @PathVariable UUID categoryId,
            Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        sessionGroupService.bindCategoryToGroup(groupId, categoryId, agent.getId());
    }

    /**
     * 解除分组的分类绑定
     * DELETE /api/v1/session-groups/{groupId}/categories/{categoryId}
     */
    @DeleteMapping("/{groupId}/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbindCategoryFromGroup(
            @PathVariable UUID groupId,
            @PathVariable UUID categoryId,
            Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        sessionGroupService.unbindCategoryFromGroup(groupId, categoryId, agent.getId());
    }

    /**
     * 获取分组绑定的所有分类
     * GET /api/v1/session-groups/{groupId}/categories
     */
    @GetMapping("/{groupId}/categories")
    public List<SessionCategoryDto> getGroupBoundCategories(@PathVariable UUID groupId) {
        List<UUID> categoryIds = sessionGroupService.getGroupBoundCategoryIds(groupId);
        return categoryIds.stream()
                .map(sessionCategoryService::getCategory)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前客服可绑定的分类列表（排除已绑定的分类）
     * GET /api/v1/session-groups/available-categories
     * 
     * 返回所有启用的分类中，当前客服尚未绑定到任何分组的分类
     */
    @GetMapping("/available-categories")
    public List<SessionCategoryDto> getAvailableCategories(Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        return sessionGroupService.getAvailableCategoriesForAgent(agent.getId())
                .stream()
                .map(category -> new SessionCategoryDto(
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
                ))
                .collect(Collectors.toList());
    }

    /**
     * 批量绑定分类到分组
     * PUT /api/v1/session-groups/{groupId}/categories
     * 请求体: { "categoryIds": ["uuid1", "uuid2"] }
     */
    @PutMapping("/{groupId}/categories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchBindCategoriesToGroup(
            @PathVariable UUID groupId,
            @RequestBody Map<String, List<UUID>> request,
            Authentication authentication) {
        Agent agent = getCurrentAgent(authentication);
        List<UUID> categoryIds = request.get("categoryIds");
        
        if (categoryIds == null) {
            return;
        }
        
        // 获取当前分组已绑定的分类
        List<UUID> currentBoundIds = sessionGroupService.getGroupBoundCategoryIds(groupId);
        
        // 解除不在新列表中的绑定
        for (UUID currentId : currentBoundIds) {
            if (!categoryIds.contains(currentId)) {
                sessionGroupService.unbindCategoryFromGroup(groupId, currentId, agent.getId());
            }
        }
        
        // 添加新的绑定
        for (UUID categoryId : categoryIds) {
            if (!currentBoundIds.contains(categoryId)) {
                sessionGroupService.bindCategoryToGroup(groupId, categoryId, agent.getId());
            }
        }
    }

    /**
     * 获取当前登录的客服
     */
    private Agent getCurrentAgent(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AgentPrincipal)) {
            throw new IllegalStateException("未登录或不是客服用户");
        }
        
        AgentPrincipal principal = (AgentPrincipal) authentication.getPrincipal();
        return agentRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("客服不存在"));
    }
}
