package com.example.aikef.service;

import com.example.aikef.dto.WebhookMessageResponse;
import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.dto.websocket.ServerEvent;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.*;
import com.example.aikef.model.enums.MessageType;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.*;
import com.example.aikef.service.strategy.AgentAssignmentStrategy;
import com.example.aikef.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import com.example.aikef.model.Attachment;
import java.time.Instant;
import java.util.*;

/**
 * 第三方平台服务
 * 处理外部平台消息接收和转发
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalPlatformService {

    private final ExternalPlatformRepository platformRepository;
    private final ExternalSessionMappingRepository mappingRepository;
    private final CustomerRepository customerRepository;
    private final ChatSessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final SessionCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final WebSocketSessionManager sessionManager;
    private final AgentAssignmentStrategy agentAssignmentStrategy;
    private final EntityMapper entityMapper;
    private final TranslationService translationService;

    @Lazy
    @Autowired
    private OfficialChannelMessageService officialChannelMessageService;

    /**
     * 处理来自第三方平台的 Webhook 消息
     */
    @Transactional
    public WebhookMessageResponse handleWebhookMessage(String platformName, WebhookMessageRequest request) {
        try {
            log.info("收到第三方平台消息: platform={}, threadId={}, content={}, language={}", 
                    platformName, request.threadId(), 
                    request.content().length() > 50 ? request.content().substring(0, 50) + "..." : request.content(),
                    request.language());

            // 1. 验证平台配置
            ExternalPlatform platform = platformRepository.findByNameAndEnabledTrue(platformName)
                    .orElseThrow(() -> new IllegalArgumentException("平台不存在或未启用: " + platformName));

            // 2. 查找或创建会话映射
            ExternalSessionMapping mapping = findOrCreateMapping(platform, request);
            boolean newSession = mapping.getCreatedAt().equals(mapping.getUpdatedAt());
            ChatSession session = mapping.getSession();

            // 3. 处理客户语言
            String customerLanguage = handleCustomerLanguage(session, request);

            // 4. 创建消息（包含翻译）
            Message message = createMessage(session, request, customerLanguage);

            // 5. 广播消息到 WebSocket
            broadcastMessageToWebSocket(session, message);

            // 6. 触发 AI 工作流（事务提交后异步执行，确保数据可见）
            final UUID sessionId = session.getId();
            final String messageContent = request.content();
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            triggerAiWorkflowIfNeeded(sessionId, messageContent,message.getId());
                        }
                    }
            );

            log.info("Webhook 消息处理成功: messageId={}, sessionId={}, newSession={}, customerLanguage={}", 
                    message.getId(), session.getId(), newSession, customerLanguage);

            return WebhookMessageResponse.success(
                    message.getId(),
                    session.getId(),
                    mapping.getCustomer().getId(),
                    newSession
            );

        } catch (Exception e) {
            log.error("Webhook 消息处理失败: platform={}, threadId={}", platformName, request.threadId(), e);
            return WebhookMessageResponse.error(e.getMessage());
        }
    }

    /**
     * 处理客户语言
     * 优先使用请求中携带的语言，否则尝试检测第一条消息的语言
     */
    private String handleCustomerLanguage(ChatSession session, WebhookMessageRequest request) {
        // 如果会话已经有语言设置，直接返回
        if (session.getCustomerLanguage() != null && !session.getCustomerLanguage().isBlank()) {
            return session.getCustomerLanguage();
        }

        String detectedLanguage = null;

        // 1. 优先使用请求中携带的语言
        if (request.language() != null && !request.language().isBlank()) {
            detectedLanguage = request.language();
            log.info("使用请求中携带的语言: {}", detectedLanguage);
        } 
        // 2. 否则自动检测消息语言
        else if (translationService.isEnabled()) {
            detectedLanguage = translationService.detectLanguage(request.content());
            log.info("自动检测到客户语言: {}", detectedLanguage);
        }

        // 3. 设置会话的客户语言
        if (detectedLanguage != null) {
            session.setCustomerLanguage(detectedLanguage);
            sessionRepository.save(session);
        }

        return detectedLanguage;
    }

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 查找或创建外部会话映射
     */
    private ExternalSessionMapping findOrCreateMapping(ExternalPlatform platform, WebhookMessageRequest request) {
        // 查找现有映射
        Optional<ExternalSessionMapping> existingMapping = mappingRepository
                .findByPlatformNameAndThreadId(platform.getName(), request.threadId());

        if (existingMapping.isPresent()) {
            ExternalSessionMapping mapping = existingMapping.get();
            boolean changed = false;
            // 更新用户信息（如果有新的）
            if (request.userName() != null && !request.userName().equals(mapping.getExternalUserName())) {
                mapping.setExternalUserName(request.userName());
                changed = true;
            }
            // 更新元数据（如果有新的）
            if (request.metadata() != null && !request.metadata().isEmpty()) {
                try {
                    Map<String, Object> currentMeta = new HashMap<>();
                    if (mapping.getMetadata() != null) {
                        currentMeta = objectMapper.readValue(mapping.getMetadata(), Map.class);
                    }
                    // 合并元数据
                    currentMeta.putAll(request.metadata());
                    String newMetaJson = objectMapper.writeValueAsString(currentMeta);
                    if (!newMetaJson.equals(mapping.getMetadata())) {
                        mapping.setMetadata(newMetaJson);
                        changed = true;
                    }
                } catch (Exception e) {
                    log.warn("更新元数据失败", e);
                }
            }
            
            if (changed) {
                return mappingRepository.save(mapping);
            }
            return mapping;
        }

        // 创建新的映射
        log.info("创建新的外部会话映射: platform={}, threadId={}", platform.getName(), request.threadId());

        // 1. 查找或创建客户
        Customer customer = findOrCreateCustomer(platform, request);

        // 2. 创建会话
        ChatSession session = createSession(customer, platform, request);

        // 3. 创建映射
        ExternalSessionMapping mapping = new ExternalSessionMapping();
        mapping.setPlatform(platform);
        mapping.setExternalThreadId(request.threadId());
        mapping.setSession(session);
        mapping.setCustomer(customer);
        mapping.setExternalUserId(request.externalUserId());
        mapping.setExternalUserName(request.userName());
        
        if (request.metadata() != null) {
            try {
                mapping.setMetadata(objectMapper.writeValueAsString(request.metadata()));
            } catch (JsonProcessingException e) {
                log.warn("序列化 metadata 失败", e);
            }
        }


        return mappingRepository.save(mapping);
    }

    /**
     * 查找或创建客户
     */
    private Customer findOrCreateCustomer(ExternalPlatform platform, WebhookMessageRequest request) {

        // 根据平台类型和 externalUserId 查找现有客户
        if (request.externalUserId() != null) {
            Optional<Customer> existing = findCustomerByPlatform(platform.getPlatformType(), request.externalUserId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Customer customer = new Customer();

        // 通过邮箱或手机查找
        if (StringUtils.isNotBlank(request.email())) {
            customer.setEmail(request.email());
            Optional<Customer> byEmail = customerRepository.findByEmail(request.email());
            if (byEmail.isPresent()) {
                // 更新平台 ID
                updateCustomerPlatformId(byEmail.get(), platform.getPlatformType(), request.externalUserId());
                return byEmail.get();
            }
        }
        if (StringUtils.isNotBlank(request.phone())) {
            customer.setPhone(request.phone());
            Optional<Customer> byPhone = customerRepository.findByPhone(request.phone());
            if (byPhone.isPresent()) {
                updateCustomerPlatformId(byPhone.get(), platform.getPlatformType(), request.externalUserId());
                return byPhone.get();
            }
        }

        // 创建新客户
        customer.setPrimaryChannel(platform.getPlatformType());
        customer.setName(request.getUserNameOrDefault());
        
        // 设置平台特定 ID
        setCustomerPlatformId(customer, platform.getPlatformType(), request.externalUserId());

        return customerRepository.save(customer);
    }

    /**
     * 根据平台类型查找客户
     */
    private Optional<Customer> findCustomerByPlatform(Channel platformType, String externalUserId) {
        if (externalUserId == null) {
            return Optional.empty();
        }
        return switch (platformType) {
            case WECHAT -> customerRepository.findByWechatOpenId(externalUserId);
            case LINE -> customerRepository.findByLineId(externalUserId);
            case WHATSAPP -> customerRepository.findByWhatsappId(externalUserId);
            case TELEGRAM -> customerRepository.findByTelegramId(externalUserId);
            case FACEBOOK -> customerRepository.findByFacebookId(externalUserId);
            case TWITTER -> Optional.empty();
            case EMAIL -> Optional.empty();
            default -> Optional.empty();
        };
    }

    /**
     * 设置客户平台 ID
     */
    private void setCustomerPlatformId(Customer customer, Channel platformType, String externalUserId) {
        if (externalUserId == null) return;
        switch (platformType) {
            case WECHAT -> customer.setWechatOpenId(externalUserId);
            case LINE -> customer.setLineId(externalUserId);
            case WHATSAPP -> customer.setWhatsappId(externalUserId);
            case TELEGRAM -> customer.setTelegramId(externalUserId);
            case FACEBOOK -> customer.setFacebookId(externalUserId);
            case EMAIL -> customer.setEmail(externalUserId);
            default -> {}
        }
    }

    /**
     * 更新客户平台 ID
     */
    private void updateCustomerPlatformId(Customer customer, Channel platformType, String externalUserId) {
        setCustomerPlatformId(customer, platformType, externalUserId);
        customerRepository.save(customer);
    }

    /**
     * 创建会话
     */
    private ChatSession createSession(Customer customer, ExternalPlatform platform, WebhookMessageRequest request) {
        ChatSession session = new ChatSession();
        session.setCustomer(customer);
        session.setStatus(SessionStatus.AI_HANDLING); // 默认 AI 处理
        session.setCreatedAt(Instant.now());
        session.setLastActiveAt(Instant.now());

        // 分配主责客服
        Agent primaryAgent = agentAssignmentStrategy.assignPrimaryAgent(
                customer,
                customer.getPrimaryChannel()
        );
        if (primaryAgent != null) {
            session.setPrimaryAgent(primaryAgent);
            log.info("为外部平台会话分配主责客服: agentId={}, agentName={}", 
                    primaryAgent.getId(), primaryAgent.getName());
        }

        // 分配支持客服（可选）
        List<Agent> supportAgents = agentAssignmentStrategy.assignSupportAgents(
                customer,
                customer.getPrimaryChannel(),
                primaryAgent
        );
        if (supportAgents != null && !supportAgents.isEmpty()) {
            List<UUID> supportAgentIds = supportAgents.stream()
                    .map(Agent::getId)
                    .toList();
            session.setSupportAgentIds(new ArrayList<>(supportAgentIds));
            log.info("为外部平台会话分配支持客服: count={}", supportAgentIds.size());
        }

        // 设置分类
        if (request.categoryId() != null) {
            try {
                UUID categoryId = UUID.fromString(request.categoryId());
                categoryRepository.findById(categoryId).ifPresent(session::setCategory);
            } catch (IllegalArgumentException e) {
                log.warn("无效的分类 ID: {}", request.categoryId());
            }
        }

        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "external_platform");
        metadata.put("platform", platform.getName());
        metadata.put("platformType", platform.getPlatformType().name());
        metadata.put("externalThreadId", request.threadId());
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        
        try {
            session.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.warn("序列化会话 metadata 失败", e);
        }


        session = sessionRepository.save(session);

        chatSessionService.assignSessionToAgentGroup(session, primaryAgent, request.categoryId()!=null?UUID.fromString(request.categoryId()):null);

        return session;
    }

    /**
     * 创建消息（旧版本，兼容）
     */
    private Message createMessage(ChatSession session, WebhookMessageRequest request) {
        return createMessage(session, request, session.getCustomerLanguage());
    }

    /**
     * 创建消息（包含翻译）
     */
    private Message createMessage(ChatSession session, WebhookMessageRequest request, String customerLanguage) {
        Message message = new Message();
        message.setSession(session);
        message.setText(request.content());
        message.setSenderType(SenderType.USER);
        message.setCreatedAt(request.timestamp() != null ? 
                Instant.ofEpochMilli(request.timestamp()) : Instant.now());
        message.setInternal(false);

        // 翻译消息内容
        if (translationService.isEnabled() && request.content() != null && !request.content().isBlank()) {
            Map<String, Object> translationData = translationService.translateMessage(
                    request.content(), 
                    customerLanguage
            );
            message.setTranslationData(translationData);
            log.debug("消息翻译完成: translationData keys={}", translationData.keySet());
        }

        // 设置消息元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("messageType", request.getMessageTypeOrDefault());
        metadata.put("source", "external_webhook");
        if (request.attachmentUrl() != null) {
            metadata.put("attachmentUrl", request.attachmentUrl());
            metadata.put("attachmentName", request.attachmentName());
        }
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        
//        try {
//            message.setMetadata(objectMapper.writeValueAsString(metadata));
//        } catch (JsonProcessingException e) {
//            log.warn("序列化消息 metadata 失败", e);
//        }

        return messageRepository.save(message);
    }

    /**
     * 广播消息到 WebSocket
     * 使用与 WebSocketEventService 相同的消息格式
     */
    private void broadcastMessageToWebSocket(ChatSession session, Message message) {
        try {
            // 使用 EntityMapper 转换为完整的 MessageDto，保持与 WebSocket 消息格式一致
            var messageDto = entityMapper.toMessageDto(message);
            
            // 构建广播事件（与 WebSocketEventService 格式一致）
            ServerEvent broadcastEvent = new ServerEvent("newMessage", Map.of(
                    "sessionId", session.getId().toString(),
                    "message", messageDto));
            
            String jsonPayload = objectMapper.writeValueAsString(broadcastEvent);
            
            // 获取会话参与者信息
            UUID customerId = session.getCustomer() != null ? session.getCustomer().getId() : null;
            UUID primaryAgentId = session.getPrimaryAgent() != null ? session.getPrimaryAgent().getId() : null;
            List<UUID> supportAgentIds = session.getSupportAgentIds() != null ? 
                    session.getSupportAgentIds().stream().toList() : null;
            
            sessionManager.broadcastToSession(
                    session.getId(),
                    primaryAgentId,
                    supportAgentIds,
                    customerId,
                    customerId,  // 发送者是客户
                    jsonPayload
            );
        } catch (Exception e) {
            log.error("广播消息到 WebSocket 失败: sessionId={}", session.getId(), e);
        }
    }

    @Autowired
    private WebSocketEventService webSocketEventService;
    
    /**
     * 触发 AI 工作流（事务提交后调用）
     */
    private void triggerAiWorkflowIfNeeded(UUID sessionId, String content,UUID messageId) {
        log.debug("事务提交后触发 AI 工作流: sessionId={}", sessionId);
        // 外部平台消息可能没有 messageId，传递 null
        webSocketEventService.triggerAiWorkflowIfNeeded(sessionId, content, messageId);
    }

    /**
     * 转发消息到第三方平台（异步）
     * 客服/AI发送的消息会根据客户语言进行翻译后转发
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param senderType 发送者类型
     */
    @Async
    public void forwardMessageToExternalPlatform(UUID sessionId, String content, SenderType senderType) {
        forwardMessageToExternalPlatform(sessionId, content, senderType, MessageType.TEXT, null);
    }

    /**
     * 转发消息到第三方平台（异步，支持附件）
     * 客服/AI发送的消息会根据客户语言进行翻译后转发
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param senderType 发送者类型
     * @param attachments 附件列表（可选）
     */
    @Async
    public void forwardMessageToExternalPlatform(UUID sessionId, String content, SenderType senderType,
                                                 List<Attachment> attachments) {
        forwardMessageToExternalPlatform(sessionId, content, senderType, MessageType.TEXT, attachments);
    }

    /**
     * 转发消息到第三方平台（异步，支持附件和消息类型）
     * 客服/AI发送的消息会根据客户语言进行翻译后转发
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param senderType 发送者类型
     * @param messageType 消息类型
     * @param attachments 附件列表（可选）
     */
    @Async
    public void forwardMessageToExternalPlatform(UUID sessionId, String content, SenderType senderType,
                                                 MessageType messageType,
                                                 List<Attachment> attachments) {
        try {
            // 查找会话的外部平台映射
            Optional<ExternalSessionMapping> mappingOpt = mappingRepository.findBySessionId(sessionId);
            if (mappingOpt.isEmpty()) {
                // 不是外部平台会话，不需要转发
                return;
            }

            ExternalSessionMapping mapping = mappingOpt.get();
            ExternalPlatform platform = mapping.getPlatform();

            if (platform.getCallbackUrl() == null || platform.getCallbackUrl().isBlank()) {
                log.debug("平台未配置回调 URL，跳过转发: platform={}", platform.getName());
                return;
            }

            // 获取会话的客户语言，如果客服/AI发送消息，尝试翻译为客户语言
            // 注意：由于是异步方法，需要直接查询数据库获取 ChatSession，避免 LazyInitializationException
            String translatedContent = content;
            ChatSession session = sessionRepository.findById(sessionId).orElse(null);
            String customerLanguage = session != null ? session.getCustomerLanguage() : null;
            
            // 处理卡片消息转换 (Fallback to text)
            if (messageType != MessageType.TEXT && content != null) {
                try {
                    Object cardData = objectMapper.readValue(content, Object.class);
                    StringBuilder sb = new StringBuilder();
                    
                    switch (messageType) {
                        case CARD_PRODUCT -> {
                            sb.append("🛍️ Product Recommendation\n");
                            sb.append("----------------\n");
                            if (cardData instanceof java.util.List) {
                                java.util.List<Map<String, Object>> products = (java.util.List<Map<String, Object>>) cardData;
                                for (int i = 0; i < products.size(); i++) {
                                    Map<String, Object> p = products.get(i);
                                    if (i > 0) sb.append("\n");
                                    if (p.get("title") != null) sb.append(p.get("title")).append("\n");
                                    if (p.get("price") != null) sb.append("Price: ").append(p.get("price")).append(" ").append(p.getOrDefault("currency", "")).append("\n");
                                    if (p.get("url") != null) sb.append("Link: ").append(p.get("url")).append("\n");
                                }
                            } else {
                                Map<String, Object> single = (Map<String, Object>) cardData;
                                if (single.get("title") != null) sb.append(single.get("title")).append("\n");
                                if (single.get("price") != null) sb.append("Price: ").append(single.get("price")).append(" ").append(single.getOrDefault("currency", "")).append("\n");
                                if (single.get("url") != null) sb.append("Link: ").append(single.get("url")).append("\n");
                            }
                        }
                        case CARD_GIFT -> {
                            Map<String, Object> gift = (Map<String, Object>) cardData;
                            sb.append("🎁 You received a Gift Card!\n");
                            sb.append("----------------\n");
                            if (gift.get("amount") != null) sb.append("Value: ").append(gift.get("amount")).append("\n");
                            if (gift.get("code") != null) sb.append("Code: ").append(gift.get("code")).append("\n");
                        }
                        case CARD_DISCOUNT -> {
                            Map<String, Object> discount = (Map<String, Object>) cardData;
                            sb.append("🎟️ Special Discount For You\n");
                            sb.append("----------------\n");
                            if (discount.get("code") != null) sb.append("Code: ").append(discount.get("code")).append("\n");
                            if (discount.get("value") != null) sb.append("Value: ").append(discount.get("value")).append("\n");
                        }
                    }
                    
                    if (sb.length() > 0) {
                        translatedContent = sb.toString();
                        // 对于卡片消息，不需要再进行机器翻译
                        customerLanguage = null; 
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse card data for external platform, falling back to raw content", e);
                }
            } else if (translationService.isEnabled() && customerLanguage != null && !customerLanguage.isBlank()
                    && (senderType == SenderType.AGENT || senderType == SenderType.AI || senderType == SenderType.SYSTEM)) {
                // 将客服/AI文本消息翻译为客户语言
                String systemLanguage = translationService.getDefaultSystemLanguage();
                translatedContent = translationService.translate(content, systemLanguage, customerLanguage);
                log.debug("消息已翻译为客户语言: {} -> {}", systemLanguage, customerLanguage);
            }

            log.info("转发消息到第三方平台: platform={}, threadId={}, senderType={}, type={}", 
                    platform.getName(), mapping.getExternalThreadId(), senderType, messageType);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("threadId", mapping.getExternalThreadId());
            requestBody.put("content", translatedContent);
            requestBody.put("originalContent", content); // 保留原文
            requestBody.put("senderType", senderType.name());
            requestBody.put("messageType", messageType != null ? messageType.name() : MessageType.TEXT.name()); // 传递消息类型
            requestBody.put("timestamp", System.currentTimeMillis());
            requestBody.put("externalUserId", mapping.getExternalUserId());
            
            // 添加附件信息（如果有）
            if (attachments != null && !attachments.isEmpty()) {
                List<Map<String, Object>> attachmentList = new ArrayList<>();
                for (Attachment attachment : attachments) {
                    Map<String, Object> attMap = new HashMap<>();
                    attMap.put("type", attachment.getType());
                    attMap.put("url", attachment.getUrl());
                    attMap.put("name", attachment.getName());
                    attMap.put("sizeKb", attachment.getSizeKb());
                    attachmentList.add(attMap);
                }
                requestBody.put("attachments", attachmentList);
            }

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 添加认证
            addAuthHeaders(headers, platform);
            
            // 添加额外请求头
            addExtraHeaders(headers, platform);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    platform.getCallbackUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("消息转发成功: platform={}, threadId={}", 
                        platform.getName(), mapping.getExternalThreadId());
            } else {
                log.warn("消息转发响应异常: platform={}, status={}, body={}", 
                        platform.getName(), response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("消息转发失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 添加认证请求头
     */
    private void addAuthHeaders(HttpHeaders headers, ExternalPlatform platform) {
        if (platform.getAuthType() == null || platform.getAuthCredential() == null) {
            return;
        }

        switch (platform.getAuthType()) {
            case API_KEY -> headers.set("X-API-Key", platform.getAuthCredential());
            case BEARER_TOKEN -> headers.setBearerAuth(platform.getAuthCredential());
            case BASIC_AUTH -> {
                // 格式: username:password
                String credentials = platform.getAuthCredential();
                headers.setBasicAuth(credentials.contains(":") ? 
                        credentials.split(":")[0] : credentials,
                        credentials.contains(":") ? 
                        credentials.split(":")[1] : "");
            }
            case CUSTOM_HEADER -> {
                // 格式: Header-Name:value
                String[] parts = platform.getAuthCredential().split(":", 2);
                if (parts.length == 2) {
                    headers.set(parts[0].trim(), parts[1].trim());
                }
            }
            default -> {}
        }
    }

    /**
     * 添加额外请求头
     */
    private void addExtraHeaders(HttpHeaders headers, ExternalPlatform platform) {
        if (platform.getExtraHeaders() == null || platform.getExtraHeaders().isBlank()) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> extraHeaders = objectMapper.readValue(
                    platform.getExtraHeaders(), Map.class);
            extraHeaders.forEach(headers::set);
        } catch (Exception e) {
            log.warn("解析额外请求头失败: platform={}", platform.getName(), e);
        }
    }



    // ==================== 平台管理接口 ====================

    /**
     * 创建平台配置
     */
    @Transactional
    public ExternalPlatform createPlatform(ExternalPlatform platform) {
        if (platformRepository.existsByName(platform.getName())) {
            throw new IllegalArgumentException("平台名称已存在: " + platform.getName());
        }
        return platformRepository.save(platform);
    }

    /**
     * 更新平台配置
     */
    @Transactional
    public ExternalPlatform updatePlatform(UUID platformId, ExternalPlatform updates) {
        ExternalPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));
        
        if (updates.getDisplayName() != null) {
            platform.setDisplayName(updates.getDisplayName());
        }
        if (updates.getCallbackUrl() != null) {
            platform.setCallbackUrl(updates.getCallbackUrl());
        }
        if (updates.getAuthType() != null) {
            platform.setAuthType(updates.getAuthType());
        }
        if (updates.getAuthCredential() != null) {
            platform.setAuthCredential(updates.getAuthCredential());
        }
        if (updates.getExtraHeaders() != null) {
            platform.setExtraHeaders(updates.getExtraHeaders());
        }
        if (updates.getWebhookSecret() != null) {
            platform.setWebhookSecret(updates.getWebhookSecret());
        }
        platform.setEnabled(updates.isEnabled());
        
        return platformRepository.save(platform);
    }

    /**
     * 获取平台配置
     */
    public Optional<ExternalPlatform> getPlatform(String name) {
        return platformRepository.findByName(name);
    }

    /**
     * 获取所有平台
     */
    public List<ExternalPlatform> getAllPlatforms() {
        return platformRepository.findAll();
    }
}

