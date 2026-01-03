package com.example.aikef.mapper;

import com.example.aikef.dto.*;
import com.example.aikef.model.*;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.SessionGroupMappingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    private static final Logger log = LoggerFactory.getLogger(EntityMapper.class);

    private final SessionGroupMappingRepository sessionGroupMappingRepository;
    private final com.example.aikef.repository.MessageRepository messageRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;
    private final com.example.aikef.repository.SpecialCustomerRepository specialCustomerRepository;

    public EntityMapper(SessionGroupMappingRepository sessionGroupMappingRepository,
                        com.example.aikef.repository.MessageRepository messageRepository,
                        AgentRepository agentRepository,
                        ObjectMapper objectMapper,
                        com.example.aikef.repository.SpecialCustomerRepository specialCustomerRepository) {
        this.sessionGroupMappingRepository = sessionGroupMappingRepository;
        this.messageRepository = messageRepository;
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
        this.specialCustomerRepository = specialCustomerRepository;
    }

    public AgentDto toAgentDto(Agent agent) {
        if (agent == null) {
            return null;
        }
        Role role = agent.getRole();
        return new AgentDto(
                agent.getId(),
                agent.getName(),
                agent.getEmail(),
                agent.getAvatarUrl(),
                agent.getStatus(),
                role != null ? role.getId() : null,
                role != null ? role.getName() : null,
                agent.getLanguage());
    }

    public RoleDto toRoleDto(Role role) {
        if (role == null) {
            return null;
        }
        Map<String, Object> permissions = role.getPermissions();
        return new RoleDto(
                role.getId(), 
                role.getName(), 
                role.getDescription(),
                role.isSystem(), 
                permissions);
    }

    

    

    /**
     * 转换为ChatSessionDto（包含指定客服的分组ID）
     * 
     * @param session 会话实体
     * @param agentId 客服ID
     * @return 会话DTO
     */
    public ChatSessionDto toChatSessionDtoForAgent(ChatSession session, UUID agentId) {
        if (session == null) {
            return null;
        }
        
        // 转换客户信息，从session.user获取notes和tags
        CustomerDto customerDto = null;
        if (session.getCustomer() != null) {
            customerDto = toCustomerDtoFromSession(session);
        }
        
        // 计算最后活跃时间戳（毫秒）
        long lastActive = session.getLastActiveAt() != null ? 
            session.getLastActiveAt().toEpochMilli() : 0;
        
        // 只查询该客服的分组映射
        UUID sessionGroupId = null;
        SessionGroupMapping mapping = sessionGroupMappingRepository
                .findBySessionIdAndAgentId(session.getId(), agentId)
                .orElse(null);
        
        if (mapping != null) {
            sessionGroupId = mapping.getSessionGroup().getId();
        }
        
        // 查询最后一条消息
        Message lastMessage = messageRepository.findFirstBySession_IdOrderByCreatedAtDesc(session.getId());
        SessionMessageDto lastMessageDto = lastMessage != null ? toSessionMessageDto(lastMessage) : null;
        
        // 构建客服列表（主要客服 + 支持客服）
        List<SessionAgentDto> agents = buildSessionAgents(session);
        
        // 获取主要客服ID
        UUID primaryAgentId = session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null;
        
        return new ChatSessionDto(
                session.getId(),
                session.getCustomer() != null ? session.getCustomer().getId() : null,
                customerDto,
                session.getStatus(),
                lastActive,
                0,
                sessionGroupId,
                primaryAgentId,
                agents,
                lastMessageDto,
                session.getNote(),
                session.getCategory() != null ? session.getCategory().getId() : null,
                session.getCategory() != null ? toSessionCategoryDto(session.getCategory()) : null,
                parseMetadata(session.getMetadata()),
                session.getCustomerLanguage()
        );
    }

    /**
     * 解析会话元数据 JSON 字符串为 Map
     */
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析会话 metadata 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建会话客服列表（主要客服在前，支持客服在后）
     */
    private List<SessionAgentDto> buildSessionAgents(ChatSession session) {
        List<SessionAgentDto> agents = new ArrayList<>();
        UUID primaryAgentId = session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null;
        
        // 添加主要客服
        if (primaryAgentId != null) {
            agentRepository.findByIdWithRole(primaryAgentId)
                    .map(agent -> SessionAgentDto.fromAgentDto(toAgentDto(agent), true))
                    .ifPresent(agents::add);
        }
        
        // 添加支持客服
        if (session.getSupportAgentIds() != null && !session.getSupportAgentIds().isEmpty()) {
            for (UUID supportAgentId : session.getSupportAgentIds()) {
                agentRepository.findByIdWithRole(supportAgentId)
                        .map(agent -> SessionAgentDto.fromAgentDto(toAgentDto(agent), false))
                        .ifPresent(agents::add);
            }
        }
        
        return agents;
    }

    public SessionCategoryDto toSessionCategoryDto(SessionCategory sessionCategory) {

        return new SessionCategoryDto(
                sessionCategory.getId(),
                sessionCategory.getName(),
                sessionCategory.getDescription(),
                sessionCategory.getIcon(),
                sessionCategory.getColor(),
                sessionCategory.getSortOrder(),
                sessionCategory.isEnabled(),
                sessionCategory.getCreatedByAgent() != null ? sessionCategory.getCreatedByAgent().getId() : null,
                sessionCategory.getCreatedAt(),
                sessionCategory.getUpdatedAt()
        );
    }
    
    /**
     * 从ChatSession转换为CustomerDto（包含来自UserProfile的notes和Customer的tags）
     * 用于在session上下文中返回客户信息
     */
    private CustomerDto toCustomerDtoFromSession(ChatSession session) {
        Customer customer = session.getCustomer();
        if (customer == null) {
            return null;
        }
        
        String notes = customer.getNotes();
        
        // 查找特殊角色
        SpecialCustomer specialCustomer = specialCustomerRepository.findByCustomer_Id(customer.getId()).orElse(null);
        String roleCode = null;
        String roleName = null;
        if (specialCustomer != null) {
            roleCode = specialCustomer.getRole().getCode();
            roleName = specialCustomer.getRole().getName();
        }
        
        return new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getPrimaryChannel(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getWechatOpenId(),
                customer.getWhatsappId(),
                customer.getLineId(),
                customer.getTelegramId(),
                customer.getFacebookId(),
                customer.getAvatarUrl(),
                customer.getLocation(),
                notes,
                customer.getCustomFields() != null ? Map.copyOf(customer.getCustomFields()) : Map.of(),
                customer.isActive(),
                customer.getLastInteractionAt(),
                customer.getCreatedAt(),
                customer.getTags() != null ? List.copyOf(customer.getTags()) : List.of(), // 手动标签
                customer.getAiTags() != null ? List.copyOf(customer.getAiTags()) : List.of(), // AI标签
                roleCode,
                roleName
        );
    }
    
    /**
     * 转换为简化的客户DTO（用于session列表）
     * @deprecated 使用 toCustomerDtoFromSession 代替
     */
    @Deprecated
    public CustomerDto toSimpleCustomerDto(Customer customer) {
        if (customer == null) {
            return null;
        }
        
        // 查找特殊角色
        SpecialCustomer specialCustomer = specialCustomerRepository.findByCustomer_Id(customer.getId()).orElse(null);
        String roleCode = null;
        String roleName = null;
        if (specialCustomer != null) {
            roleCode = specialCustomer.getRole().getCode();
            roleName = specialCustomer.getRole().getName();
        }
        
        return new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getPrimaryChannel(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getWechatOpenId(),
                customer.getWhatsappId(),
                customer.getLineId(),
                customer.getTelegramId(),
                customer.getFacebookId(),
                customer.getAvatarUrl(),
                customer.getLocation(),
                customer.getNotes(),
                customer.getCustomFields() != null ? Map.copyOf(customer.getCustomFields()) : Map.of(),
                customer.isActive(),
                customer.getLastInteractionAt(),
                customer.getCreatedAt(),
                customer.getTags() != null ? List.copyOf(customer.getTags()) : List.of(),
                customer.getAiTags() != null ? List.copyOf(customer.getAiTags()) : List.of(),
                roleCode,
                roleName
        );
    }
    
    /**
     * 转换 SessionGroup 到 DTO
     */
    public SessionGroupDto toSessionGroupDto(SessionGroup group) {
        if (group == null) {
            return null;
        }
        return new SessionGroupDto(
                group.getId(),
                group.getName(),
                group.isSystem(),
                group.getAgent() != null ? group.getAgent().getId() : null,
                group.getIcon(),
                group.getColor(),
                group.getSortOrder(),
                List.of(), // 默认空列表，由 Service 层填充
                List.of(), // 默认空列表，由 Service 层填充
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
    
    /**
     * 转换 SessionGroup 到 DTO（带 sessions）
     */
    public SessionGroupDto toSessionGroupDtoWithSessions(SessionGroup group, List<ChatSessionDto> sessions) {
        if (group == null) {
            return null;
        }
        return new SessionGroupDto(
                group.getId(),
                group.getName(),
                group.isSystem(),
                group.getAgent() != null ? group.getAgent().getId() : null,
                group.getIcon(),
                group.getColor(),
                group.getSortOrder(),
                sessions != null ? sessions : List.of(),
                List.of(), // 默认空列表，由 Service 层填充
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    /**
     * 转换 SessionGroup 到 DTO（带 sessions 和绑定的分类列表）
     */
    public SessionGroupDto toSessionGroupDtoWithSessionsAndCategories(
            SessionGroup group, 
            List<ChatSessionDto> sessions, 
            List<SessionCategoryDto> categories) {
        if (group == null) {
            return null;
        }
        return new SessionGroupDto(
                group.getId(),
                group.getName(),
                group.isSystem(),
                group.getAgent() != null ? group.getAgent().getId() : null,
                group.getIcon(),
                group.getColor(),
                group.getSortOrder(),
                sessions != null ? sessions : List.of(),
                categories != null ? categories : List.of(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
    
    private SessionMessageDto toSessionMessageDto(Message message) {
        if (message == null) {
            return null;
        }
        List<AttachmentDto> attachments = message.getAttachments() != null
                ? message.getAttachments().stream()
                    .map(this::toAttachmentDto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
                : List.of();
        List<String> mentions = message.getMentionAgentIds() != null
                ? List.copyOf(message.getMentionAgentIds())
                : List.of();
        long timestamp = message.getCreatedAt() != null ? message.getCreatedAt().toEpochMilli() : 0L;
        String sender = message.getSenderType() != null ? message.getSenderType().name() : null;
        return new SessionMessageDto(
                message.getId(),
                message.getText(),
                sender,
                timestamp,
                message.isInternal(),
                attachments,
                mentions
        );
    }

    public MessageDto toMessageDto(Message message) {
        if (message == null) {
            return null;
        }
        List<AttachmentDto> attachments = message.getAttachments().stream()
                .map(this::toAttachmentDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new MessageDto(
                message.getId(),
                message.getSession() != null ? message.getSession().getId() : null,
                message.getSenderType(),
                message.getAgent() != null ? message.getAgent().getId() : null,
                message.getText(),
                message.isInternal(),
                message.getTranslationData(),
                List.copyOf(message.getMentionAgentIds()),
                attachments,
                message.getCreatedAt());
    }

    public AttachmentDto toAttachmentDto(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return new AttachmentDto(
                attachment.getId(),
                attachment.getType(),
                attachment.getUrl(),
                attachment.getName(),
                attachment.getSizeKb());
    }

    public QuickReplyDto toQuickReplyDto(QuickReply reply) {
        if (reply == null) {
            return null;
        }
        return new QuickReplyDto(reply.getId(), reply.getLabel(), reply.getText(), reply.getCategory(), reply.isSystem());
    }

    public KnowledgeEntryDto toKnowledgeEntryDto(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return null;
        }
        return new KnowledgeEntryDto(
                knowledgeBase.getId(), 
                knowledgeBase.getName(), 
                knowledgeBase.getDescription(),
                knowledgeBase.getDocumentCount(),
                knowledgeBase.getEnabled()
        );
    }

    public CustomerDto toCustomerDto(Customer customer) {
        if (customer == null) {
            return null;
        }
        
        // 查找特殊角色
        SpecialCustomer specialCustomer = specialCustomerRepository.findByCustomer_Id(customer.getId()).orElse(null);
        String roleCode = null;
        String roleName = null;
        if (specialCustomer != null) {
            roleCode = specialCustomer.getRole().getCode();
            roleName = specialCustomer.getRole().getName();
        }
        
        return new CustomerDto(
                customer.getId(),
                customer.getName(),
                customer.getPrimaryChannel(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getWechatOpenId(),
                customer.getWhatsappId(),
                customer.getLineId(),
                customer.getTelegramId(),
                customer.getFacebookId(),
                customer.getAvatarUrl(),
                customer.getLocation(),
                customer.getNotes(),
                customer.getCustomFields() != null ? Map.copyOf(customer.getCustomFields()) : Map.of(),
                customer.isActive(),
                customer.getLastInteractionAt(),
                customer.getCreatedAt(),
                customer.getTags() != null ? List.copyOf(customer.getTags()) : List.of(), // 手动标签
                customer.getAiTags() != null ? List.copyOf(customer.getAiTags()) : List.of(), // AI标签
                roleCode,
                roleName
        );
    }
}
