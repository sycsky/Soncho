package com.example.aikef.controller;

import com.example.aikef.dto.SessionCategoryDto;
import com.example.aikef.dto.request.CreateSessionCategoryRequest;
import com.example.aikef.dto.request.UpdateSessionCategoryRequest;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.SessionCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 会话分类管理接口
 */
@RestController
@RequestMapping("/api/v1/session-categories")
public class SessionCategoryController {

    private final SessionCategoryService sessionCategoryService;
    private final AgentRepository agentRepository;

    public SessionCategoryController(SessionCategoryService sessionCategoryService,
                                    AgentRepository agentRepository) {
        this.sessionCategoryService = sessionCategoryService;
        this.agentRepository = agentRepository;
    }

    /**
     * 获取所有启用的分类
     * GET /api/v1/session-categories
     */
    @GetMapping
    public List<SessionCategoryDto> getEnabledCategories() {
        return sessionCategoryService.getAllEnabledCategories();
    }

    /**
     * 获取所有分类（包括禁用的，管理员用）
     * GET /api/v1/session-categories/all
     */
    @GetMapping("/all")
    public List<SessionCategoryDto> getAllCategories() {
        return sessionCategoryService.getAllCategories();
    }

    /**
     * 获取分类详情
     * GET /api/v1/session-categories/{id}
     */
    @GetMapping("/{id}")
    public SessionCategoryDto getCategory(@PathVariable UUID id) {
        return sessionCategoryService.getCategory(id);
    }

    /**
     * 创建分类（需要管理员权限）
     * POST /api/v1/session-categories
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionCategoryDto createCategory(
            @RequestBody CreateSessionCategoryRequest request,
            Authentication authentication) {
        UUID createdByAgentId = getCurrentAgentId(authentication);
        return sessionCategoryService.createCategory(request, createdByAgentId);
    }

    /**
     * 更新分类（需要管理员权限）
     * PUT /api/v1/session-categories/{id}
     */
    @PutMapping("/{id}")
    public SessionCategoryDto updateCategory(
            @PathVariable UUID id,
            @RequestBody UpdateSessionCategoryRequest request,
            Authentication authentication) {
        getCurrentAgentId(authentication); // 验证登录
        return sessionCategoryService.updateCategory(id, request);
    }

    /**
     * 删除分类（需要管理员权限）
     * DELETE /api/v1/session-categories/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @PathVariable UUID id,
            Authentication authentication) {
        getCurrentAgentId(authentication); // 验证登录
        sessionCategoryService.deleteCategory(id);
    }

    /**
     * 获取当前登录的客服ID
     */
    private UUID getCurrentAgentId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AgentPrincipal)) {
            throw new IllegalStateException("未登录或不是客服用户");
        }
        
        AgentPrincipal principal = (AgentPrincipal) authentication.getPrincipal();
        return principal.getId();
    }
}

