package com.example.aikef.controller;

import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.service.OfficialChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 官方渠道配置API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/official-channels")
@RequiredArgsConstructor
public class OfficialChannelController {

    private final OfficialChannelService channelService;

    /**
     * 获取所有官方渠道配置
     */
    @GetMapping("/configs")
    public List<OfficialChannelConfig> getAllConfigs() {
        return channelService.getAllConfigs();
    }

    /**
     * 获取指定渠道配置
     */
    @GetMapping("/configs/{channelType}")
    public ResponseEntity<OfficialChannelConfig> getConfig(
            @PathVariable OfficialChannelConfig.ChannelType channelType) {
        return channelService.getConfig(channelType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 保存或更新官方渠道配置
     * 
     * 请求示例（微信服务号）:
     * POST /api/v1/official-channels/configs
     * {
     *   "channelType": "WECHAT_OFFICIAL",
     *   "configData": {
     *     "appId": "wx1234567890abcdef",
     *     "appSecret": "secret123456",
     *     "token": "your_token",
     *     "encodingAESKey": "your_encoding_key"
     *   },
     *   "webhookSecret": "your_webhook_secret",
     *   "enabled": true,
     *   "remark": "微信服务号配置"
     * }
     * 
     * 请求示例（Line）:
     * {
     *   "channelType": "LINE_OFFICIAL",
     *   "configData": {
     *     "channelId": "1234567890",
     *     "channelSecret": "secret123456",
     *     "channelAccessToken": "access_token_here"
     *   },
     *   "webhookSecret": "your_webhook_secret",
     *   "enabled": true
     * }
     * 
     * 请求示例（WhatsApp）:
     * {
     *   "channelType": "WHATSAPP_OFFICIAL",
     *   "configData": {
     *     "phoneNumberId": "123456789012345",
     *     "accessToken": "your_access_token",
     *     "businessAccountId": "123456789012345",
     *     "appId": "your_app_id",
     *     "appSecret": "your_app_secret"
     *   },
     *   "webhookSecret": "your_webhook_secret",
     *   "enabled": true
     * }
     */
    @PostMapping("/configs")
    public OfficialChannelConfig saveOrUpdateConfig(@Valid @RequestBody SaveConfigRequest request) {
        return channelService.saveOrUpdateConfig(
                request.channelType(),
                request.configData(),
                request.webhookSecret(),
                request.enabled(),
                request.remark(),
                request.categoryId()
        );
    }

    /**
     * 启用/禁用官方渠道
     */
    @PatchMapping("/configs/{channelType}/toggle")
    public OfficialChannelConfig toggleChannel(
            @PathVariable OfficialChannelConfig.ChannelType channelType,
            @RequestParam boolean enabled) {
        return channelService.toggleChannel(channelType, enabled);
    }

    /**
     * 删除官方渠道配置
     */
    @DeleteMapping("/configs/{channelType}")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable OfficialChannelConfig.ChannelType channelType) {
        channelService.deleteConfig(channelType);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取所有渠道类型
     */
    @GetMapping("/channel-types")
    public List<ChannelTypeInfo> getChannelTypes() {
        return List.of(
                new ChannelTypeInfo("WECHAT_OFFICIAL", "微信服务号"),
                new ChannelTypeInfo("WECHAT_KF", "微信客服"),
                new ChannelTypeInfo("LINE_OFFICIAL", "Line官方账号"),
                new ChannelTypeInfo("WHATSAPP_OFFICIAL", "WhatsApp Business"),
                new ChannelTypeInfo("FACEBOOK_MESSENGER", "Facebook Messenger"),
                new ChannelTypeInfo("INSTAGRAM", "Instagram Direct"),
                new ChannelTypeInfo("TELEGRAM", "Telegram"),
                new ChannelTypeInfo("TWITTER", "X (Twitter)"),
                new ChannelTypeInfo("DOUYIN", "抖音"),
                new ChannelTypeInfo("RED_BOOK", "小红书"),
                new ChannelTypeInfo("WEIBO", "微博"),
                new ChannelTypeInfo("EMAIL", "邮件")
        );
    }

    // ==================== 请求/响应记录 ====================

    public record SaveConfigRequest(
            OfficialChannelConfig.ChannelType channelType,
            Map<String, Object> configData,
            String webhookSecret,
            boolean enabled,
            String remark,
            java.util.UUID categoryId  // 会话分类ID（可选）
    ) {}

    public record ChannelTypeInfo(String value, String label) {}
}
