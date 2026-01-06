package com.example.aikef.service;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.ExternalPlatform;
import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.ExternalPlatformRepository;
import com.example.aikef.repository.ExternalSessionMappingRepository;
import com.example.aikef.repository.OfficialChannelConfigRepository;
import com.example.aikef.service.channel.wechat.WechatOfficialAdapter;
import com.example.aikef.service.channel.line.LineOfficialAdapter;
import com.example.aikef.service.channel.whatsapp.WhatsappOfficialAdapter;
import com.example.aikef.service.channel.facebook.FacebookAdapter;
import com.example.aikef.service.channel.telegram.TelegramAdapter;
import com.example.aikef.service.channel.twitter.TwitterAdapter;
import com.example.aikef.service.channel.douyin.DouyinAdapter;
import com.example.aikef.service.channel.redbook.RedBookAdapter;
import com.example.aikef.service.channel.weibo.WeiboAdapter;
import com.example.aikef.service.channel.email.EmailAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final ExternalPlatformRepository platformRepository;
    private final ExternalPlatformService externalPlatformService;
    private final ObjectMapper objectMapper;
    
    // 官方渠道适配器（通过SDK发送消息）
    private final WechatOfficialAdapter wechatAdapter;
    private final LineOfficialAdapter lineAdapter;
    private final WhatsappOfficialAdapter whatsappAdapter;
    private final FacebookAdapter facebookAdapter;
    private final TelegramAdapter telegramAdapter;
    private final TwitterAdapter twitterAdapter;
    private final DouyinAdapter douyinAdapter;
    private final RedBookAdapter redBookAdapter;
    private final WeiboAdapter weiboAdapter;
    private final EmailAdapter emailAdapter;

    @Autowired
    private final StringRedisTemplate redisTemplate;

    /**
     * 验证微信Webhook（GET请求）
     */
    public ResponseEntity<String> verifyWechatWebhook(String signature, String timestamp, 
                                                      String nonce, String echostr) {
        return verifyWechatWebhookInternal(
                OfficialChannelConfig.ChannelType.WECHAT_OFFICIAL,
                "wechat_official",
                signature,
                timestamp,
                nonce,
                echostr
        );
    }

    public ResponseEntity<String> verifyWechatKfWebhook(String signature, String timestamp,
                                                        String nonce, String echostr) {
        return verifyWechatWebhookInternal(
                OfficialChannelConfig.ChannelType.WECHAT_KF,
                "wechat_kf",
                signature,
                timestamp,
                nonce,
                echostr
        );
    }

    /**
     * 处理微信服务号消息（POST请求）
     */
    @Transactional
    public ResponseEntity<String> handleWechatMessage(String body, String signature, 
                                                       String timestamp, String nonce) {
        return handleWechatMessageInternal(
                OfficialChannelConfig.ChannelType.WECHAT_OFFICIAL,
                "wechat_official",
                body,
                signature,
                timestamp,
                nonce
        );
    }

    @Transactional
    public ResponseEntity<String> handleWechatKfMessage(String body, String signature,
                                                        String timestamp, String nonce) {
        return handleWechatMessageInternal(
                OfficialChannelConfig.ChannelType.WECHAT_KF,
                "wechat_kf",
                body,
                signature,
                timestamp,
                nonce
        );
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
        } else if ("wechat_kf".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.WECHAT_KF;
        } else if ("line_official".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.LINE_OFFICIAL;
        } else if ("whatsapp_official".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.WHATSAPP_OFFICIAL;
        } else if ("facebook_messenger".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.FACEBOOK_MESSENGER;
        } else if ("instagram".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.INSTAGRAM;
        } else if ("telegram".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.TELEGRAM;
        } else if ("twitter".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.TWITTER;
        } else if ("douyin".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.DOUYIN;
        } else if ("red_book".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.RED_BOOK;
        } else if ("weibo".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.WEIBO;
        } else if ("email".equals(platformName)) {
            channelType = OfficialChannelConfig.ChannelType.EMAIL;
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
                case WECHAT_KF -> {
                    String openKfId = null;
                    if (mapping.getMetadata() != null) {
                        try {
                            Map<String, Object> meta = objectMapper.readValue(mapping.getMetadata(), Map.class);
                            openKfId = (String) meta.get("open_kfid");
                        } catch (Exception e) {
                            log.error("解析元数据失败: sessionId={}", sessionId, e);
                        }
                    }
                    if (openKfId == null) {
                        log.error("微信客服消息发送失败: 缺少open_kfid, sessionId={}", sessionId);
                        return false;
                    }
                    wechatAdapter.sendKfMessage(config, openKfId, externalUserId, content, attachments);
                }
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

    private ResponseEntity<String> verifyWechatWebhookInternal(
            OfficialChannelConfig.ChannelType channelType,
            String platformName,
            String signature,
            String timestamp,
            String nonce,
            String echostr
    ) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(channelType)
                .orElse(null);

        if (config == null) {
            log.warn("微信渠道未配置或未启用: channelType={}", channelType);
            return ResponseEntity.badRequest().body("配置不存在");
        }

        ensureExternalPlatformExists(platformName, config);

        // 微信客服（企业微信）验证逻辑：
        // 1. 签名验证包含 echostr
        // 2. 验证成功后需要解密 echostr 返回明文
        boolean isValid;
        if (channelType == OfficialChannelConfig.ChannelType.WECHAT_KF) {
            isValid = wechatAdapter.verifySignature(config, signature, timestamp, nonce, echostr);
        } else {
            isValid = wechatAdapter.verifySignature(config, signature, timestamp, nonce);
        }

        if (!isValid) {
            log.warn("微信Webhook验证失败: channelType={}, signature={}, timestamp={}, nonce={}, echostr={}", 
                    channelType, signature, timestamp, nonce, echostr);
            return ResponseEntity.badRequest().body("验证失败");
        }

        String responseStr = echostr;
        // 微信客服需要返回解密后的明文
        if (channelType == OfficialChannelConfig.ChannelType.WECHAT_KF) {
            try {
                responseStr = wechatAdapter.decryptEchostr(config, echostr);
                log.info("微信客服 echostr 解密成功: {}", responseStr);
            } catch (Exception e) {
                log.error("微信客服 echostr 解密失败", e);
                return ResponseEntity.badRequest().body("解密失败");
            }
        }

        log.info("微信Webhook验证成功: channelType={}", channelType);
        return ResponseEntity.ok(responseStr);
    }

    private ResponseEntity<String> handleWechatMessageInternal(
            OfficialChannelConfig.ChannelType channelType,
            String platformName,
            String body,
            String signature,
            String timestamp,
            String nonce
    ) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(channelType)
                .orElse(null);

        if (config == null) {
            log.warn("微信渠道未配置或未启用: channelType={}", channelType);
            return ResponseEntity.badRequest().body("配置不存在");
        }

        ensureExternalPlatformExists(platformName, config);

        try {
            // 微信客服（企业微信）POST请求验证逻辑：
            // 1. 需要从 body 中提取 Encrypt 字段
            // 2. 将 Encrypt 字段参与签名计算
            // 3. 验证成功后解密消息
            String messageBody = body;
            
            if (channelType == OfficialChannelConfig.ChannelType.WECHAT_KF) {
                String encrypt = wechatAdapter.extractEncrypt(body);
                if (encrypt != null) {
                    // 如果存在加密字段，使用加密字段进行签名验证
                    if (!wechatAdapter.verifySignature(config, signature, timestamp, nonce, encrypt)) {
                        log.warn("微信客服签名验证失败: signature={}, encrypt={}", signature, encrypt);
                        return ResponseEntity.badRequest().body("签名验证失败");
                    }
                    // 解密消息
                    try {
                        messageBody = wechatAdapter.decryptMessage(config, encrypt);
                        log.debug("微信客服消息解密成功");
                    } catch (Exception e) {
                        log.error("微信客服消息解密失败", e);
                        return ResponseEntity.badRequest().body("解密失败");
                    }
                } else {
                    // 如果没有加密字段，尝试普通验证（可能是不加密模式或异常情况）
                    if (!wechatAdapter.verifySignature(config, signature, timestamp, nonce)) {
                        return ResponseEntity.badRequest().body("签名验证失败");
                    }
                }
            } else {
                // 普通微信服务号验证
                if (!wechatAdapter.verifySignature(config, signature, timestamp, nonce)) {
                    return ResponseEntity.badRequest().body("签名验证失败");
                }
            }

            Map<String, Object> wechatMessage = wechatAdapter.parseMessage(messageBody, config);
            
            // 处理微信客服的同步消息事件
            if (channelType == OfficialChannelConfig.ChannelType.WECHAT_KF) {
                String msgType = (String) wechatMessage.get("MsgType");
                String event = (String) wechatMessage.get("Event");
                if ("event".equals(msgType) && "kf_msg_or_event".equals(event)) {
                    String token = (String) wechatMessage.get("Token");
                    if (token != null) {
                        log.info("收到微信客服消息事件，开始同步消息: token={}", token);
                        
                        // 获取上次同步的 cursor
                        // 从 Redis 获取 cursor，key: wechat:kf:cursor:{appId}
                        String cursorKey = "wechat:kf:cursor:" + config.getId();
                        String cursor = redisTemplate.opsForValue().get(cursorKey);
                        if (cursor == null) {
                            cursor = "";
                        }
                        
                        // 同步消息
                        WechatOfficialAdapter.SyncResult result = wechatAdapter.syncMessages(config, token, cursor);
                        
                        for (WebhookMessageRequest req : result.messages()) {
                            externalPlatformService.handleWebhookMessage(platformName, req);
                        }
                        
                        // 更新 cursor 到 Redis
                        if (result.nextCursor() != null && !result.nextCursor().equals(cursor)) {
                            redisTemplate.opsForValue().set(cursorKey, result.nextCursor());
                            log.info("更新微信客服 cursor (Redis): key={}, cursor={}", cursorKey, result.nextCursor());
                        }
                        
                        return ResponseEntity.ok("success");
                    }
                }
            }

            WebhookMessageRequest request = wechatAdapter.toWebhookRequest(wechatMessage);
            if (request == null) {
                return ResponseEntity.ok("success");
            }

            if (config.getCategoryId() != null && request.categoryId() == null) {
                request = new WebhookMessageRequest(
                        request.threadId(),
                        request.content(),
                        request.messageType(),
                        request.externalUserId(),
                        request.userName(),
                        request.email(),
                        request.phone(),
                        config.getCategoryId().toString(),
                        request.attachmentUrl(),
                        request.attachmentName(),
                        request.timestamp(),
                        request.language(),
                        request.metadata()
                );
            }

            var response = externalPlatformService.handleWebhookMessage(platformName, request);
            if (response.success()) {
                return ResponseEntity.ok("success");
            }
            return ResponseEntity.badRequest().body(response.errorMessage());
        } catch (Exception e) {
            log.error("处理微信消息失败: channelType={}", channelType, e);
            return ResponseEntity.badRequest().body("处理失败: " + e.getMessage());
        }
    }

    private void ensureExternalPlatformExists(String platformName, OfficialChannelConfig config) {
        if (platformRepository.existsByName(platformName)) {
            return;
        }

        ExternalPlatform platform = new ExternalPlatform();
        platform.setName(platformName);
        platform.setDisplayName(config.getDisplayName() != null ? config.getDisplayName() : platformName);
        platform.setPlatformType(switch (config.getChannelType()) {
            case WECHAT_OFFICIAL, WECHAT_KF -> com.example.aikef.model.Channel.WECHAT;
            case LINE_OFFICIAL -> com.example.aikef.model.Channel.LINE;
            case WHATSAPP_OFFICIAL -> com.example.aikef.model.Channel.WHATSAPP;
            case FACEBOOK_MESSENGER -> com.example.aikef.model.Channel.FACEBOOK;
            case INSTAGRAM -> com.example.aikef.model.Channel.INSTAGRAM;
            case TELEGRAM -> com.example.aikef.model.Channel.TELEGRAM;
            case TWITTER -> com.example.aikef.model.Channel.TWITTER;
            case EMAIL -> com.example.aikef.model.Channel.EMAIL;
            case DOUYIN -> com.example.aikef.model.Channel.DOUYIN;
            case RED_BOOK -> com.example.aikef.model.Channel.REDBOOK;
            case WEIBO -> com.example.aikef.model.Channel.WEIBO;
            default -> com.example.aikef.model.Channel.OTHER;
        });
        platform.setEnabled(true);
        platform.setWebhookSecret(config.getWebhookSecret());
        platformRepository.save(platform);
    }

    // ==================== Facebook / Instagram ====================

    public ResponseEntity<String> verifyFacebookWebhook(OfficialChannelConfig.ChannelType channelType, String mode, String token, String challenge) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(channelType)
                .orElse(null);

        if (config == null) {
            return ResponseEntity.status(403).body("Config not found");
        }

        if (facebookAdapter.verifyWebhook(config, mode, token, challenge)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }

    @Transactional
    public ResponseEntity<String> handleFacebookMessage(OfficialChannelConfig.ChannelType channelType, String body, String signature) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(channelType)
                .orElse(null);

        if (config == null) return ResponseEntity.badRequest().body("Config not found");
        
        // TODO: Validate signature (X-Hub-Signature-256)

        Map<String, Object> message = facebookAdapter.parseMessage(body);
        WebhookMessageRequest request = facebookAdapter.toWebhookRequest(message);
        
        if (request != null) {
            String platformName = channelType == OfficialChannelConfig.ChannelType.INSTAGRAM ? "instagram" : "facebook_messenger";
            ensureExternalPlatformExists(platformName, config);
            externalPlatformService.handleWebhookMessage(platformName, request);
        }
        
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    // ==================== Telegram ====================

    @Transactional
    public ResponseEntity<String> handleTelegramMessage(String body) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType.TELEGRAM)
                .orElse(null);

        if (config == null) return ResponseEntity.badRequest().body("Config not found");

        Map<String, Object> message = telegramAdapter.parseMessage(body);
        WebhookMessageRequest request = telegramAdapter.toWebhookRequest(message);

        if (request != null) {
            ensureExternalPlatformExists("telegram", config);
            externalPlatformService.handleWebhookMessage("telegram", request);
        }

        return ResponseEntity.ok("OK");
    }

    // ==================== Twitter ====================

    public ResponseEntity<Map<String, String>> verifyTwitterCrc(String crcToken) {
        OfficialChannelConfig config = configRepository
                .findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType.TWITTER)
                .orElse(null);

        if (config == null) return ResponseEntity.badRequest().build();

        String response = twitterAdapter.generateCrcResponse(config, crcToken);
        if (response != null) {
            return ResponseEntity.ok(Map.of("response_token", response));
        }
        return ResponseEntity.badRequest().build();
    }

    @Transactional
    public ResponseEntity<String> handleTwitterMessage(String body) {
        // Implement logic
        return ResponseEntity.ok("OK");
    }
    
    // ==================== Douyin, RedBook, Weibo, Email ====================
    
    @Transactional
    public ResponseEntity<String> handleDouyinMessage(String body, String signature) {
        // Implement logic
        return ResponseEntity.ok("OK");
    }

    @Transactional
    public ResponseEntity<String> handleRedBookMessage(String body, String signature) {
        // Implement logic
        return ResponseEntity.ok("OK");
    }

    @Transactional
    public ResponseEntity<String> handleWeiboMessage(String body, String signature) {
        // Implement logic
        return ResponseEntity.ok("OK");
    }
    
    @Transactional
    public ResponseEntity<String> handleEmailMessage(String body) {
        // Implement logic
        return ResponseEntity.ok("OK");
    }
}
