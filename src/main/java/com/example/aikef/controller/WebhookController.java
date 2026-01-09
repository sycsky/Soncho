package com.example.aikef.controller;

import com.example.aikef.dto.WebhookMessageResponse;
import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.Channel;
import com.example.aikef.model.ExternalPlatform;
import com.example.aikef.service.ExternalPlatformService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Webhook 接口控制器
 * 接收来自第三方平台（Line、WhatsApp、微信等）的消息
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ExternalPlatformService externalPlatformService;

    /**
     * 接收第三方平台消息
     * 
     * URL 格式: POST /api/v1/webhook/{platformName}/message
     * 
     * @param platformName 平台名称（如: line, whatsapp, wechat）
     * @param request      消息请求体
     * @return 处理结果
     * 
     * 示例请求:
     * POST /api/v1/webhook/line/message
     * {
     *   "threadId": "U1234567890abcdef",
     *   "content": "你好，我想咨询一下订单问题",
     *   "externalUserId": "U1234567890abcdef",
     *   "userName": "张三",
     *   "messageType": "text",
     *   "timestamp": 1702345678000,
     *   "metadata": {
     *     "replyToken": "xxx",
     *     "source": "line_official"
     *   }
     * }
     */
    @PostMapping("/{platformName}/message")
    public ResponseEntity<WebhookMessageResponse> receiveMessage(
            @PathVariable String platformName,
            @Valid @RequestBody WebhookMessageRequest request) {
        
        log.info("收到 Webhook 请求: platform={}, threadId={}", platformName, request.threadId());
        
        WebhookMessageResponse response = externalPlatformService.handleWebhookMessage(platformName, request);
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Webhook 验证接口（用于平台验证）
     * 某些平台（如微信）需要先验证 webhook URL
     */
    @GetMapping("/{platformName}/verify")
    public ResponseEntity<String> verifyWebhook(
            @PathVariable String platformName,
            @RequestParam(required = false) String echostr,
            @RequestParam(required = false) String signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce) {
        
        log.info("Webhook 验证请求: platform={}", platformName);
        
        // 简单返回 echostr 完成验证（微信公众号验证方式）
        if (echostr != null) {
            return ResponseEntity.ok(echostr);
        }
        
        return ResponseEntity.ok("OK");
    }

    // ==================== 平台管理接口 ====================

    /**
     * 获取所有平台配置
     */
    @GetMapping("/platforms")
    public List<ExternalPlatform> getAllPlatforms() {
        return externalPlatformService.getAllPlatforms();
    }

    /**
     * 创建平台配置
     * 
     * 示例请求:
     * POST /api/v1/webhook/platforms
     * {
     *   "name": "line",
     *   "displayName": "Line Official Account",
     *   "platformType": "LINE",
     *   "callbackUrl": "https://api.line.me/v2/bot/message/push",
     *   "authType": "BEARER_TOKEN",
     *   "authCredential": "your-channel-access-token",
     *   "enabled": true
     * }
     */
    @PostMapping("/platforms")
    public ExternalPlatform createPlatform(@Valid @RequestBody ExternalPlatform platform) {
        return externalPlatformService.createPlatform(platform);
    }

    /**
     * 更新平台配置
     */
    @PutMapping("/platforms/{platformId}")
    public ExternalPlatform updatePlatform(
            @PathVariable UUID platformId,
            @RequestBody ExternalPlatform updates) {
        return externalPlatformService.updatePlatform(platformId, updates);
    }

    /**
     * 获取平台配置
     */
    @GetMapping("/platforms/{platformName}")
    public ResponseEntity<ExternalPlatform> getPlatform(@PathVariable String platformName) {
        return externalPlatformService.getPlatform(platformName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 枚举值接口 ====================

    /**
     * 获取所有平台类型
     */
    @GetMapping("/platform-types")
    public List<PlatformTypeInfo> getPlatformTypes() {
        return Arrays.stream(Channel.values())
                .map(type -> new PlatformTypeInfo(type.name(), getDisplayName(type)))
                .toList();
    }

    /**
     * 获取所有认证类型
     */
    @GetMapping("/auth-types")
    public List<AuthTypeInfo> getAuthTypes() {
        return Arrays.stream(ExternalPlatform.AuthType.values())
                .map(type -> new AuthTypeInfo(type.name(), getAuthTypeDescription(type)))
                .toList();
    }

    private String getDisplayName(Channel type) {
        return switch (type) {
            case LINE -> "Line";
            case WHATSAPP -> "WhatsApp";
            case WECHAT -> "微信";
            case TELEGRAM -> "Telegram";
            case FACEBOOK -> "Facebook Messenger";
            case TWITTER -> "Twitter";
            case EMAIL -> "Email";
            case WEB -> "网页";
            case CUSTOM -> "自定义";
            case OTHER -> "其他";
            case INSTAGRAM -> "Instagram";
            case DOUYIN -> "抖音";
            case REDBOOK -> "小红书";
            case WEIBO -> "微博";
            case SMS -> "短信";
            case PHONE -> "电话";
            case APP -> "APP";
        };
    }

    private String getAuthTypeDescription(ExternalPlatform.AuthType type) {
        return switch (type) {
            case NONE -> "无认证";
            case API_KEY -> "API Key (X-API-Key 请求头)";
            case BEARER_TOKEN -> "Bearer Token";
            case BASIC_AUTH -> "Basic 认证";
            case CUSTOM_HEADER -> "自定义请求头";
        };
    }

    /**
     * 平台类型信息
     */
    public record PlatformTypeInfo(String value, String label) {}

    /**
     * 认证类型信息
     */
    public record AuthTypeInfo(String value, String description) {}
}

