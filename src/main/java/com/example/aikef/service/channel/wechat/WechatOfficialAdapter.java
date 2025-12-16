package com.example.aikef.service.channel.wechat;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.service.OfficialChannelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信服务号适配器
 * 处理微信服务号的消息接收和发送（通过微信官方SDK）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatOfficialAdapter {

    private final OfficialChannelService channelService;
    private final ObjectMapper objectMapper;

    /**
     * 验证微信Webhook签名
     */
    public boolean verifySignature(OfficialChannelConfig config, String signature, 
                                   String timestamp, String nonce) {
        // TODO: 实现微信签名验证逻辑
        // 1. 获取token（从configJson中解析）
        // 2. 将token、timestamp、nonce按字典序排序后拼接
        // 3. SHA1加密
        // 4. 与signature比较
        
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String token = (String) configData.get("token");
        
        if (token == null) {
            log.warn("微信配置中缺少token");
            return false;
        }
        
        // 简单实现（实际需要使用微信SDK或实现完整签名算法）
        log.info("验证微信签名: signature={}, timestamp={}, nonce={}", signature, timestamp, nonce);
        return true; // 临时返回true，需要实现完整逻辑
    }

    /**
     * 解析微信消息
     */
    public Map<String, Object> parseMessage(String body, OfficialChannelConfig config) {
        try {
            // TODO: 使用微信SDK解析XML消息
            // 微信消息是XML格式，需要解析为Map
            
            log.info("解析微信消息: body长度={}", body.length());
            // 临时实现：返回空Map，需要实现XML解析
            return Map.of();
        } catch (Exception e) {
            log.error("解析微信消息失败", e);
            throw new RuntimeException("解析微信消息失败", e);
        }
    }

    /**
     * 将微信消息转换为统一的WebhookMessageRequest格式
     */
    public WebhookMessageRequest toWebhookRequest(Map<String, Object> wechatMessage) {
        // TODO: 从微信消息Map中提取字段，转换为WebhookMessageRequest
        // 微信消息字段：FromUserName, ToUserName, MsgType, Content, CreateTime等
        
        return new WebhookMessageRequest(
                (String) wechatMessage.getOrDefault("FromUserName", ""), // threadId
                (String) wechatMessage.getOrDefault("Content", ""),      // content
                "text",  // messageType
                (String) wechatMessage.getOrDefault("FromUserName", ""), // externalUserId
                (String) wechatMessage.getOrDefault("FromUserName", ""), // userName（需要从用户信息获取）
                null,    // email
                null,    // phone
                null,    // categoryId
                null,    // attachmentUrl
                null,    // attachmentName
                System.currentTimeMillis(), // timestamp
                null,    // language
                wechatMessage // metadata
        );
    }

    /**
     * 发送消息到微信服务号（通过微信官方SDK）
     */
    public void sendMessage(OfficialChannelConfig config, String openId, String content) {
        // TODO: 使用微信SDK发送消息
        // 1. 从configJson中获取appId、appSecret
        // 2. 获取access_token（或使用缓存的）
        // 3. 调用微信API发送消息
        // POST https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=ACCESS_TOKEN
        
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String appId = (String) configData.get("appId");
        String appSecret = (String) configData.get("appSecret");
        
        log.info("发送消息到微信服务号: appId={}, openId={}, content={}", 
                appId, openId, content.length() > 50 ? content.substring(0, 50) + "..." : content);
        
        // TODO: 实现微信消息发送逻辑
        // 需要使用微信SDK或直接调用微信API
    }
}

