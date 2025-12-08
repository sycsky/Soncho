package com.example.aikef.controller;

import com.example.aikef.dto.QuickReplyDto;
import com.example.aikef.dto.request.CreateQuickReplyRequest;
import com.example.aikef.dto.request.UpdateQuickReplyRequest;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.QuickReplyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 快捷回复管理 API
 * 客服可以管理自己的快捷回复
 * 系统预设的快捷回复不可删除和修改
 */
@RestController
@RequestMapping("/api/v1/quick-replies")
public class QuickReplyController {

    private final QuickReplyService quickReplyService;

    public QuickReplyController(QuickReplyService quickReplyService) {
        this.quickReplyService = quickReplyService;
    }

    /**
     * 获取所有快捷回复
     * 包括系统预设和当前客服创建的
     */
    @GetMapping
    public List<QuickReplyDto> getAllReplies(Authentication authentication) {
        UUID agentId = getAgentId(authentication);
        return quickReplyService.getAllReplies(agentId);
    }

    /**
     * 获取单个快捷回复
     */
    @GetMapping("/{id}")
    public QuickReplyDto getReply(@PathVariable UUID id) {
        return quickReplyService.getReply(id);
    }

    /**
     * 创建快捷回复
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuickReplyDto createReply(
            @Valid @RequestBody CreateQuickReplyRequest request,
            Authentication authentication) {
        
        UUID agentId = getAgentId(authentication);
        return quickReplyService.createReply(
                request.label(),
                request.text(),
                request.category(),
                agentId,
                request.system()
        );
    }

    /**
     * 更新快捷回复
     * 只能更新自己创建的非系统回复
     */
    @PutMapping("/{id}")
    public QuickReplyDto updateReply(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuickReplyRequest request,
            Authentication authentication) {
        
        UUID agentId = getAgentId(authentication);
        return quickReplyService.updateReply(
                id,
                request.label(),
                request.text(),
                request.category(),
                agentId
        );
    }

    /**
     * 删除快捷回复
     * 只能删除自己创建的非系统回复
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID agentId = getAgentId(authentication);
        quickReplyService.deleteReply(id, agentId);
    }

    private UUID getAgentId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("需要客服认证");
    }
}
