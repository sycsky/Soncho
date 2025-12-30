package com.example.aikef.service.channel.wechat;

import com.example.aikef.dto.request.WebhookMessageRequest;
import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.service.OfficialChannelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.aikef.model.Attachment;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

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
    private final RestTemplate restTemplate;

    private static final ConcurrentHashMap<String, TokenCacheEntry> ACCESS_TOKEN_CACHE = new ConcurrentHashMap<>();

    private record TokenCacheEntry(String accessToken, long expiresAtEpochMs) {}

    /**
     * 验证微信Webhook签名
     */
    public boolean verifySignature(OfficialChannelConfig config, String signature, 
                                   String timestamp, String nonce) {
        if (signature == null || timestamp == null || nonce == null) {
            return false;
        }

        Map<String, Object> configData = channelService.parseConfigJson(config);
        String token = (String) configData.get("token");
        
        if (token == null || token.isBlank()) {
            return false;
        }

        List<String> parts = new ArrayList<>(3);
        parts.add(token);
        parts.add(timestamp);
        parts.add(nonce);
        Collections.sort(parts);
        String toHash = String.join("", parts);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            String hashed = HexFormat.of().formatHex(digest);
            return hashed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("验证微信签名失败", e);
            return false;
        }
    }

    /**
     * 解析微信消息
     */
    public Map<String, Object> parseMessage(String body, OfficialChannelConfig config) {
        try {
            if (body == null || body.isBlank()) {
                return Map.of();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            Node root = doc.getDocumentElement();
            if (root == null) {
                return Map.of();
            }

            Map<String, Object> map = new java.util.HashMap<>();
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                Node node = root.getChildNodes().item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                map.put(node.getNodeName(), node.getTextContent());
            }

            return map;
        } catch (Exception e) {
            log.error("解析微信消息失败", e);
            throw new RuntimeException("解析微信消息失败", e);
        }
    }

    /**
     * 将微信消息转换为统一的WebhookMessageRequest格式
     */
    public WebhookMessageRequest toWebhookRequest(Map<String, Object> wechatMessage) {
        if (wechatMessage == null || wechatMessage.isEmpty()) {
            return null;
        }

        String msgType = (String) wechatMessage.get("MsgType");
        if (msgType == null || msgType.isBlank()) {
            msgType = "text";
        }

        if ("event".equalsIgnoreCase(msgType)) {
            return null;
        }

        String fromUser = (String) wechatMessage.getOrDefault("FromUserName", "");

        String content = "";
        String attachmentUrl = null;
        String attachmentName = null;

        if ("text".equalsIgnoreCase(msgType)) {
            content = (String) wechatMessage.getOrDefault("Content", "");
        } else if ("image".equalsIgnoreCase(msgType)) {
            attachmentUrl = (String) wechatMessage.get("PicUrl");
            attachmentName = "image";
            content = attachmentUrl != null ? attachmentUrl : "";
        } else if ("voice".equalsIgnoreCase(msgType)) {
            content = (String) wechatMessage.getOrDefault("Recognition", "");
            if (content == null || content.isBlank()) {
                content = (String) wechatMessage.getOrDefault("MediaId", "");
            }
        } else {
            content = (String) wechatMessage.getOrDefault("Content", "");
        }

        long timestampMs = System.currentTimeMillis();
        Object createTime = wechatMessage.get("CreateTime");
        if (createTime instanceof String ct && !ct.isBlank()) {
            try {
                timestampMs = Long.parseLong(ct) * 1000L;
            } catch (Exception ignored) {
            }
        }

        return new WebhookMessageRequest(
                fromUser,
                content,
                msgType.toLowerCase(),
                fromUser,
                fromUser,
                null,    // email
                null,    // phone
                null,    // categoryId
                attachmentUrl,
                attachmentName,
                timestampMs,
                null,    // language
                wechatMessage // metadata
        );
    }

    /**
     * 发送消息到微信服务号（通过微信官方SDK）
     * 
     * @param config 配置
     * @param openId 用户openId
     * @param content 消息内容
     * @param attachments 附件列表（可选）
     */
    public void sendMessage(OfficialChannelConfig config, String openId, String content, 
                           List<Attachment> attachments) {
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String appId = (String) configData.get("appId");
        String appSecret = (String) configData.get("appSecret");

        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException("微信配置缺少appId/appSecret");
        }
        if (openId == null || openId.isBlank()) {
            throw new IllegalArgumentException("openId不能为空");
        }

        if (content == null) {
            content = "";
        }

        String accessToken = getAccessToken(appId, appSecret);
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/cgi-bin/message/custom/send")
                .queryParam("access_token", accessToken)
                .toUriString();

        Map<String, Object> payload = Map.of(
                "touser", openId,
                "msgtype", "text",
                "text", Map.of("content", content)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.postForObject(
                url,
                new HttpEntity<>(payload, headers),
                Map.class
        );

        if (resp == null) {
            throw new IllegalStateException("微信返回为空");
        }

        Object errCodeObj = resp.get("errcode");
        int errCode = errCodeObj instanceof Number n ? n.intValue() : 0;
        if (errCode != 0) {
            String errMsg = String.valueOf(resp.get("errmsg"));
            throw new IllegalStateException("微信发送失败: errcode=" + errCode + ", errmsg=" + errMsg);
        }
    }

    private String getAccessToken(String appId, String appSecret) {
        long now = System.currentTimeMillis();
        TokenCacheEntry cached = ACCESS_TOKEN_CACHE.get(appId);
        if (cached != null && cached.expiresAtEpochMs() - now > 120_000) {
            return cached.accessToken();
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
        if (resp == null) {
            throw new IllegalStateException("获取access_token失败: 微信返回为空");
        }

        Object accessTokenObj = resp.get("access_token");
        if (accessTokenObj == null) {
            String errCode = String.valueOf(resp.get("errcode"));
            String errMsg = String.valueOf(resp.get("errmsg"));
            throw new IllegalStateException("获取access_token失败: errcode=" + errCode + ", errmsg=" + errMsg);
        }

        String accessToken = String.valueOf(accessTokenObj);
        Object expiresInObj = resp.get("expires_in");
        long expiresInSeconds = expiresInObj instanceof Number n ? n.longValue() : 7200L;
        long expiresAt = now + expiresInSeconds * 1000L;
        ACCESS_TOKEN_CACHE.put(appId, new TokenCacheEntry(accessToken, expiresAt));
        return accessToken;
    }
}
