package com.example.aikef.service;

import com.example.aikef.dto.AgentDto;
import com.example.aikef.dto.request.CreateAgentRequest;
import com.example.aikef.dto.request.CreateTenantAdminRequest;
import com.example.aikef.dto.request.UpdateAgentRequest;
import com.example.aikef.dto.billing.SubscriptionDto;
import com.example.aikef.model.enums.SubscriptionPlan;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Agent;
import com.example.aikef.model.Role;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.RoleRepository;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.CurrentAgentProvider;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional(readOnly = true)
public class AgentService {

    private final AgentRepository agentRepository;
    private final RoleRepository roleRepository;
    private final EntityMapper entityMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentAgentProvider currentAgentProvider;
    private final SubscriptionService subscriptionService;
    private final SessionGroupService sessionGroupService;

    @Autowired
    @Lazy
    private com.example.aikef.workflow.service.AiWorkflowService aiWorkflowService;

    public AgentService(AgentRepository agentRepository,
                        RoleRepository roleRepository,
                        EntityMapper entityMapper,
                        PasswordEncoder passwordEncoder,
                        CurrentAgentProvider currentAgentProvider,
                        SubscriptionService subscriptionService,
                        SessionGroupService sessionGroupService) {
        this.agentRepository = agentRepository;
        this.roleRepository = roleRepository;
        this.entityMapper = entityMapper;
        this.passwordEncoder = passwordEncoder;
        this.currentAgentProvider = currentAgentProvider;
        this.subscriptionService = subscriptionService;
        this.sessionGroupService = sessionGroupService;
    }

    public Page<AgentDto> listAgents(String name, String role, Pageable pageable) {
        Specification<Agent> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.join("role").get("name"), role));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return agentRepository.findAll(spec, pageable)
                .map(entityMapper::toAgentDto);
    }

    public AgentDto currentAgent() {
        UUID agentId = currentAgentProvider.currentAgent()
                .map(AgentPrincipal::getId)
                .orElseThrow(() -> new EntityNotFoundException("未登录"));
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("坐席不存在"));
        return entityMapper.toAgentDto(agent);
    }

    @Transactional
    public AgentDto createAgent(CreateAgentRequest request) {
        // 校验邮箱唯一性
        if (agentRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("该邮箱已被使用");
        }
        
        // Check Seat Limit
        String tenantId = null;
        if (request instanceof CreateTenantAdminRequest tenantAdminRequest) {
            tenantId = tenantAdminRequest.getTenantId();
        } else {
            tenantId = currentAgentProvider.currentAgent().map(AgentPrincipal::getTenantId).orElse(null);
        }

        if (tenantId != null) {
            SubscriptionDto sub = subscriptionService.getCurrentSubscription(tenantId);
            long currentCount = agentRepository.countByTenantId(tenantId);
            if (sub.getPlan() != SubscriptionPlan.ENTERPRISE && currentCount >= sub.getPlan().getSeatLimit()) {
                throw new IllegalStateException("Seat limit reached (" + currentCount + "/" + sub.getPlan().getSeatLimit() + "). Please upgrade your plan.");
            }
        }
        
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("角色不存在"));
        Agent agent = new Agent();
        agent.setName(request.getName());
        agent.setEmail(request.getEmail());
        agent.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        agent.setRole(role);
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            agent.setLanguage(request.getLanguage());
        }
        
        if (request instanceof CreateTenantAdminRequest tenantAdminRequest) {
            agent.setTenantId(tenantAdminRequest.getTenantId());
        }

        Agent saved = agentRepository.save(agent);
        sessionGroupService.ensureDefaultGroups(saved);


        return entityMapper.toAgentDto(saved);
    }

    @Transactional
    public void updatePassword(UUID agentId, String oldPassword, String newPassword) {
        Agent agent = findById(agentId);
        
        if (!passwordEncoder.matches(oldPassword, agent.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        
        agent.setPasswordHash(passwordEncoder.encode(newPassword));
        agentRepository.save(agent);
    }

    @Transactional
    public AgentDto updateProfile(UUID agentId, String newEmail, String avatarUrl, String name) {
        Agent agent = findById(agentId);
        
        if (newEmail != null && !newEmail.equals(agent.getEmail())) {
            if (agentRepository.findByEmail(newEmail).isPresent()) {
                throw new IllegalArgumentException("该邮箱已被使用");
            }
            agent.setEmail(newEmail);
        }
        
        if (avatarUrl != null) {
            agent.setAvatarUrl(avatarUrl);
        }
        
        if (name != null && !name.isBlank()) {
            agent.setName(name);
        }
        
        Agent updated = agentRepository.save(agent);
        return entityMapper.toAgentDto(updated);
    }

    @Transactional
    public AgentDto updateAgent(UUID agentId, UpdateAgentRequest request) {
        Agent agent = findById(agentId);
        
        // 如果要更新邮箱，校验邮箱唯一性
        if (request.email() != null && !request.email().equals(agent.getEmail())) {
            if (agentRepository.findByEmail(request.email()).isPresent()) {
                throw new IllegalArgumentException("该邮箱已被使用");
            }
            agent.setEmail(request.email());
        }
        
        if (request.name() != null) {
            agent.setName(request.name());
        }
        if (request.status() != null) {
            agent.setStatus(request.status());
        }
        if (request.roleId() != null) {
            Role role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new EntityNotFoundException("角色不存在"));
            agent.setRole(role);
        }
        if (request.language() != null) {
            agent.setLanguage(request.language().isBlank() ? null : request.language());
        }
        Agent updated = agentRepository.save(agent);
        return entityMapper.toAgentDto(updated);
    }

    @Transactional
    public void deleteAgent(UUID agentId) {
        Agent agent = findById(agentId);
        
        // Check if agent is Administrator
        if (agent.getRole() != null && "Administrator".equalsIgnoreCase(agent.getRole().getName())) {
            throw new IllegalStateException("无法删除管理员角色的客服");
        }
        
        agentRepository.delete(agent);
    }

    public Agent findById(UUID id) {
        // 使用 findById (EntityManager.find) 默认不应用 Hibernate Filter
        // 需要改用 JPA Specification 或 JPQL 查询才能生效
        // 这里暂时改为使用 Specification 方式
        return agentRepository.findOne((root, query, cb) -> cb.equal(root.get("id"), id))
                .orElseThrow(() -> new EntityNotFoundException("坐席不存在"));
    }

    public AgentDto getAgent(UUID id) {
        return entityMapper.toAgentDto(findById(id));
    }
}
