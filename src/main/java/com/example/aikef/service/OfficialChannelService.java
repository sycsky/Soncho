package com.example.aikef.service;

import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.repository.OfficialChannelConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 官方渠道服务
 * 管理官方渠道配置和消息处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialChannelService {

    private final OfficialChannelConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    /**
     * 保存或更新官方渠道配置
     * 如果存在则更新，不存在则创建
     */
    @Transactional
    public OfficialChannelConfig saveOrUpdateConfig(OfficialChannelConfig.ChannelType channelType, 
                                                    Map<String, Object> configData,
                                                    String webhookSecret,
                                                    boolean enabled,
                                                    String remark,
                                                    java.util.UUID categoryId) {
        Optional<OfficialChannelConfig> existingOpt = configRepository.findByChannelType(channelType);
        
        OfficialChannelConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            log.info("更新官方渠道配置: channelType={}", channelType);
        } else {
            config = new OfficialChannelConfig();
            config.setChannelType(channelType);
            log.info("创建官方渠道配置: channelType={}", channelType);
        }

        // 设置显示名称
        String displayName = getDisplayName(channelType);
        config.setDisplayName(displayName);

        // 序列化配置数据为JSON
        if (configData != null && !configData.isEmpty()) {
            try {
                config.setConfigJson(objectMapper.writeValueAsString(configData));
            } catch (JsonProcessingException e) {
                log.error("序列化配置数据失败: channelType={}", channelType, e);
                throw new IllegalArgumentException("配置数据格式错误", e);
            }
        }

        config.setWebhookSecret(webhookSecret);
        config.setEnabled(enabled);
        config.setRemark(remark);
        
        // 设置分类ID（如果提供了categoryId）
        config.setCategoryId(categoryId);
        if (categoryId != null) {
            log.info("设置官方渠道分类: channelType={}, categoryId={}", 
                    channelType, categoryId);
        }
        
        // 设置Webhook URL（系统固定URL）
        config.setWebhookUrl("/api/v1/official-channels/" + channelType.name().toLowerCase() + "/webhook");

        return configRepository.save(config);
    }

    /**
     * 获取官方渠道配置
     */
    public Optional<OfficialChannelConfig> getConfig(OfficialChannelConfig.ChannelType channelType) {
        return configRepository.findByChannelType(channelType);
    }

    /**
     * 获取启用的官方渠道配置
     */
    public Optional<OfficialChannelConfig> getEnabledConfig(OfficialChannelConfig.ChannelType channelType) {
        return configRepository.findByChannelTypeAndEnabledTrue(channelType);
    }

    /**
     * 获取所有官方渠道配置
     */
    public List<OfficialChannelConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * 启用/禁用官方渠道
     */
    @Transactional
    public OfficialChannelConfig toggleChannel(OfficialChannelConfig.ChannelType channelType, boolean enabled) {
        OfficialChannelConfig config = configRepository.findByChannelType(channelType)
                .orElseThrow(() -> new IllegalArgumentException("渠道配置不存在: " + channelType));
        
        config.setEnabled(enabled);
        return configRepository.save(config);
    }

    /**
     * 删除官方渠道配置
     */
    @Transactional
    public void deleteConfig(OfficialChannelConfig.ChannelType channelType) {
        configRepository.findByChannelType(channelType)
                .ifPresent(configRepository::delete);
    }

    /**
     * 解析配置JSON为Map
     */
    public Map<String, Object> parseConfigJson(OfficialChannelConfig config) {
        if (config.getConfigJson() == null || config.getConfigJson().isBlank()) {
            return Map.of();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = objectMapper.readValue(
                    config.getConfigJson(), 
                    Map.class);
            return configMap;
        } catch (JsonProcessingException e) {
            log.error("解析配置JSON失败: channelType={}", config.getChannelType(), e);
            return Map.of();
        }
    }

    /**
     * 获取渠道显示名称
     */
    private String getDisplayName(OfficialChannelConfig.ChannelType channelType) {
        return switch (channelType) {
            case WECHAT_OFFICIAL -> "微信服务号";
            case WECHAT_KF -> "微信客服";
            case LINE_OFFICIAL -> "Line官方账号";
            case WHATSAPP_OFFICIAL -> "WhatsApp Business";
            case FACEBOOK_MESSENGER -> "Facebook Messenger";
            case INSTAGRAM -> "Instagram Direct";
            case TELEGRAM -> "Telegram";
            case TWITTER -> "X (Twitter)";
            case DOUYIN -> "抖音";
            case RED_BOOK -> "小红书";
            case WEIBO -> "微博";
            case EMAIL -> "邮件";
        };
    }
}
