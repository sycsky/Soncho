package com.example.aikef.service;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.ExternalSessionMappingRepository;
import com.example.aikef.repository.OfficialChannelConfigRepository;
import com.example.aikef.service.channel.wechat.WechatOfficialAdapter;
import com.example.aikef.service.channel.line.LineOfficialAdapter;
import com.example.aikef.service.channel.whatsapp.WhatsappOfficialAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 官方渠道消息服务
 * 处理官方渠道的消息接收和发送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialChannelMessageService {

    private final OfficialChannelConfigRepository configRepository;
    private final ExternalSessionMappingRepository mappingRepository;
    private final ExternalPlatformService externalPlatformService;
    private final ObjectMapper objectMapper;
    
    // 官方渠道适配器（通过SDK发送消息）
    private final WechatOfficialAdapter wechatAdapter;
    private final LineOfficialAdapter lineAdapter;
    private final WhatsappOfficialAdapter whatsappAdapter;

    /**
     * 验证微信Webhook（GET请求）
     */
    public ResponseEntity<String> verifyWechatWebhook(String signature, String timestamp, 
                                                      String nonce, String echostr) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType.WECHAT_OFFICIAL)
                .orElse(null);
        
        if (config == null) {
            log.warn("微信服务号未配置或未启用");
            return ResponseEntity.badRequest().body("配置不存在");
        }
        
        // 验证签名（需要实现微信签名验证逻辑）
        boolean isValid = wechatAdapter.verifySignature(
                config, signature, timestamp, nonce);
        
        if (isValid) {
            log.info("微信Webhook验证成功");
            return ResponseEntity.ok(echostr);
        } else {
            log.warn("微信Webhook验证失败");
            return ResponseEntity.badRequest().body("验证失败");
        }
    }

    /**
     * 处理微信服务号消息（POST请求）
     */
    @Transactional
    public ResponseEntity<String> handleWechatMessage(String body, String signature, 
                                                       String timestamp, String nonce) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType.WECHAT_OFFICIAL)
                .orElse(null);
        
        if (config == null) {
            log.warn("微信服务号未配置或未启用");
            return ResponseEntity.badRequest().body("配置不存在");
        }
        
        try {
            // 解析微信消息
            Map<String, Object> wechatMessage = wechatAdapter.parseMessage(body, config);
            
            // 转换为统一的WebhookMessageRequest格式
            WebhookMessageRequest request = wechatAdapter.toWebhookRequest(wechatMessage);
            
            // 如果配置了分类，设置到request中
            if (config.getCategoryId() != null && request.categoryId() == null) {
                request = new WebhookMessageRequest(
                        request.threadId(),
                        request.content(),
                        request.messageType(),
                        request.externalUserId(),
                        request.userName(),
                        request.email(),
                        request.phone(),
                        config.getCategoryId().toString(), // categoryId
                        request.attachmentUrl(),
                        request.attachmentName(),
                        request.timestamp(),
                        request.language(),
                        request.metadata()
                );
                log.debug("从配置中设置分类: channelType={}, categoryId={}", 
                        config.getChannelType(), config.getCategoryId());
            }
            
            // 使用ExternalPlatformService处理消息（复用现有逻辑）
            var response = externalPlatformService.handleWebhookMessage("wechat_official", request);
            
            if (response.success()) {
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.badRequest().body(response.errorMessage());
            }
            
        } catch (Exception e) {
            log.error("处理微信消息失败", e);
            return ResponseEntity.badRequest().body("处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理Line官方账号消息
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> handleLineMessage(String body, String signature) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType.LINE_OFFICIAL)
                .orElse(null);
        
        if (config == null) {
            log.warn("Line官方账号未配置或未启用");
            return ResponseEntity.badRequest().body(Map.of("error", "配置不存在"));
        }
        
        try {
            // 验证签名
            if (!lineAdapter.verifySignature(config, body, signature)) {
                log.warn("Line Webhook签名验证失败");
                return ResponseEntity.badRequest().body(Map.of("error", "签名验证失败"));
            }
            
            // 解析Line消息
            Map<String, Object> lineMessage = lineAdapter.parseMessage(body);
            
            // 转换为统一的WebhookMessageRequest格式
            WebhookMessageRequest request = lineAdapter.toWebhookRequest(lineMessage);
            
            // 如果配置了分类，设置到request中
            if (config.getCategoryId() != null && request.categoryId() == null) {
                request = new WebhookMessageRequest(
                        request.threadId(),
                        request.content(),
                        request.messageType(),
                        request.externalUserId(),
                        request.userName(),
                        request.email(),
                        request.phone(),
                        config.getCategoryId().toString(), // categoryId
                        request.attachmentUrl(),
                        request.attachmentName(),
                        request.timestamp(),
                        request.language(),
                        request.metadata()
                );
                log.debug("从配置中设置分类: channelType={}, categoryId={}", 
                        config.getChannelType(), config.getCategoryId());
            }
            
            // 使用ExternalPlatformService处理消息
            var response = externalPlatformService.handleWebhookMessage("line_official", request);
            
            if (response.success()) {
                return ResponseEntity.ok(Map.of("status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", response.errorMessage()));
            }
            
        } catch (Exception e) {
            log.error("处理Line消息失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理WhatsApp Business消息
     */
    @Transactional
    public ResponseEntity<Map<String, Object>> handleWhatsappMessage(String body, String signature) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType.WHATSAPP_OFFICIAL)
                .orElse(null);
        
        if (config == null) {
            log.warn("WhatsApp Business未配置或未启用");
            return ResponseEntity.badRequest().body(Map.of("error", "配置不存在"));
        }
        
        try {
            // 验证签名
            if (!whatsappAdapter.verifySignature(config, body, signature)) {
                log.warn("WhatsApp Webhook签名验证失败");
                return ResponseEntity.badRequest().body(Map.of("error", "签名验证失败"));
            }
            
            // 解析WhatsApp消息
            Map<String, Object> whatsappMessage = whatsappAdapter.parseMessage(body);
            
            // 转换为统一的WebhookMessageRequest格式
            WebhookMessageRequest request = whatsappAdapter.toWebhookRequest(whatsappMessage);
            
            // 如果配置了分类，设置到request中
            if (config.getCategoryId() != null && request.categoryId() == null) {
                request = new WebhookMessageRequest(
                        request.threadId(),
                        request.content(),
                        request.messageType(),
                        request.externalUserId(),
                        request.userName(),
                        request.email(),
                        request.phone(),
                        config.getCategoryId().toString(), // categoryId
                        request.attachmentUrl(),
                        request.attachmentName(),
                        request.timestamp(),
                        request.language(),
                        request.metadata()
                );
                log.debug("从配置中设置分类: channelType={}, categoryId={}", 
                        config.getChannelType(), config.getCategoryId());
            }
            
            // 使用ExternalPlatformService处理消息
            var response = externalPlatformService.handleWebhookMessage("whatsapp_official", request);
            
            if (response.success()) {
                return ResponseEntity.ok(Map.of("status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", response.errorMessage()));
            }
            
        } catch (Exception e) {
            log.error("处理WhatsApp消息失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 发送消息到官方渠道（通过官方SDK）
     * 当客服/AI发送消息时，转发到官方渠道
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param senderType 发送者类型
     * @return true 如果是官方渠道且发送成功，false 如果不是官方渠道
     */
    public boolean sendMessageToOfficialChannel(UUID sessionId, String content, SenderType senderType) {
        return sendMessageToOfficialChannel(sessionId, content, senderType, null);
    }

    /**
     * 发送消息到官方渠道（通过官方SDK，支持附件）
     * 当客服/AI发送消息时，转发到官方渠道
     * 
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param senderType 发送者类型
     * @param attachments 附件列表（可选）
     * @return true 如果是官方渠道且发送成功，false 如果不是官方渠道
     */
    public boolean sendMessageToOfficialChannel(UUID sessionId, String content, SenderType senderType, 
                                                java.util.List<com.example.aikef.model.Attachment> attachments) {
        // 查找会话的外部平台映射（复用ExternalSessionMapping）
        var mappingOpt = mappingRepository.findBySessionId(sessionId);
        if (mappingOpt.isEmpty()) {
            return false; // 不是外部平台会话
        }
        
        var mapping = mappingOpt.get();
        String platformName = mapping.getPlatform().getName();
        
        // 判断是否为官方渠道
        OfficialChannelConfig.ChannelType channelType = null;
        if ("wechat_official".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.WECHAT_OFFICIAL;
        } else if ("line_official".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.LINE_OFFICIAL;
        } else if ("whatsapp_official".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.WHATSAPP_OFFICIAL;
        }
        
        if (channelType == null) {
            return false; // 不是官方渠道
        }
        
        // 获取官方渠道配置
        var configOpt = configRepository.findByChannelTypeAndEnabledTrue(channelType);
        if (configOpt.isEmpty()) {
            log.warn("官方渠道未配置或未启用: channelType={}", channelType);
            return false;
        }
        
        OfficialChannelConfig config = configOpt.get();
        String externalUserId = mapping.getExternalUserId();
        String externalThreadId = mapping.getExternalThreadId();
        
        try {
            // 根据渠道类型调用对应的适配器发送消息（支持附件）
            switch (channelType) {
                case WECHAT_OFFICIAL -> 
                    wechatAdapter.sendMessage(config, externalUserId, content, attachments);
                case LINE_OFFICIAL -> 
                    lineAdapter.sendMessage(config, externalThreadId != null ? externalThreadId : externalUserId, content, attachments);
                case WHATSAPP_OFFICIAL -> 
                    whatsappAdapter.sendMessage(config, externalUserId, content, attachments);
            }
            
            log.info("消息已发送到官方渠道: channelType={}, externalUserId={}, attachments={}", 
                    channelType, externalUserId, attachments != null ? attachments.size() : 0);
            return true;
            
        } catch (Exception e) {
            log.error("发送消息到官方渠道失败: channelType={}, sessionId={}", 
                    channelType, sessionId, e);
            return false;
        }
    }
}

