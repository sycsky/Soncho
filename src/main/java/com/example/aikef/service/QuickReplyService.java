package com.example.aikef.service;

import com.example.aikef.dto.QuickReplyDto;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Agent;
import com.example.aikef.model.QuickReply;
import com.example.aikef.repository.QuickReplyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 快捷回复服务
 */
@Service
@Transactional(readOnly = true)
public class QuickReplyService {

    private final QuickReplyRepository quickReplyRepository;
    private final AgentService agentService;
    private final EntityMapper entityMapper;

    public QuickReplyService(QuickReplyRepository quickReplyRepository,
                            AgentService agentService,
                            EntityMapper entityMapper) {
        this.quickReplyRepository = quickReplyRepository;
        this.agentService = agentService;
        this.entityMapper = entityMapper;
    }

    /**
     * 获取所有快捷回复
     * 包括系统预设和当前客服创建的
     */
    public List<QuickReplyDto> getAllReplies(UUID agentId) {
        List<QuickReply> systemReplies = quickReplyRepository.findBySystemTrue();
        List<QuickReply> agentReplies = quickReplyRepository.findAll().stream()
                .filter(r -> !r.isSystem() && r.getCreatedBy() != null && r.getCreatedBy().getId().equals(agentId))
                .collect(Collectors.toList());

        List<QuickReply> allReplies = new java.util.ArrayList<>(systemReplies);
        allReplies.addAll(agentReplies);

        return allReplies.stream()
                .map(entityMapper::toQuickReplyDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个快捷回复
     */
    public QuickReplyDto getReply(UUID id) {
        QuickReply reply = findById(id);
        return entityMapper.toQuickReplyDto(reply);
    }

    /**
     * 创建快捷回复
     */
    @Transactional
    public QuickReplyDto createReply(String label, String text, String category, UUID agentId, Boolean system) {
        Agent agent = agentService.findById(agentId);

        QuickReply reply = new QuickReply();
        reply.setLabel(label);
        reply.setText(text);
        reply.setCategory(category);
        reply.setSystem(system != null? system : false);
        reply.setCreatedBy(agent);

        QuickReply saved = quickReplyRepository.save(reply);
        return entityMapper.toQuickReplyDto(saved);
    }

    /**
     * 更新快捷回复
     * 系统预设的不能修改
     */
    @Transactional
    public QuickReplyDto updateReply(UUID id, String label, String text, String category, UUID agentId) {
        QuickReply reply = findById(id);

        // 检查权限:只能修改自己创建的非系统回复
        if (reply.isSystem()) {
            throw new IllegalStateException("系统预设快捷回复不可修改");
        }

        if (reply.getCreatedBy() == null || !reply.getCreatedBy().getId().equals(agentId)) {
            throw new IllegalStateException("只能修改自己创建的快捷回复");
        }

        reply.setLabel(label);
        reply.setText(text);
        reply.setCategory(category);

        QuickReply updated = quickReplyRepository.save(reply);
        return entityMapper.toQuickReplyDto(updated);
    }

    /**
     * 删除快捷回复
     * 系统预设的不能删除
     */
    @Transactional
    public void deleteReply(UUID id, UUID agentId) {
        QuickReply reply = findById(id);

        // 检查权限:只能删除自己创建的非系统回复
        if (reply.isSystem()) {
            throw new IllegalStateException("系统预设快捷回复不可删除");
        }

        if (reply.getCreatedBy() == null || !reply.getCreatedBy().getId().equals(agentId)) {
            throw new IllegalStateException("只能删除自己创建的快捷回复");
        }

        quickReplyRepository.delete(reply);
    }

    private QuickReply findById(UUID id) {
        return quickReplyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("快捷回复不存在"));
    }
}
