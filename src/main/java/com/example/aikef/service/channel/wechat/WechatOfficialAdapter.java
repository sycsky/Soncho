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
    private final WechatAesUtil aesUtil;

    private static final ConcurrentHashMap<String, TokenCacheEntry> ACCESS_TOKEN_CACHE = new ConcurrentHashMap<>();

    private record TokenCacheEntry(String accessToken, long expiresAtEpochMs) {}

    /**
     * 获取 Access Token
     */
    public String getAccessToken(OfficialChannelConfig config) {
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String appId = (String) configData.get("appId"); // corpid
        String appSecret = (String) configData.get("appSecret"); // corpsecret

        if (appId == null || appSecret == null) {
            throw new RuntimeException("WeChat configuration missing appId or appSecret");
        }

        TokenCacheEntry entry = ACCESS_TOKEN_CACHE.get(appId);
        if (entry != null && entry.expiresAtEpochMs > System.currentTimeMillis()) {
            return entry.accessToken;
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/gettoken")
                .queryParam("corpid", appId)
                .queryParam("corpsecret", appSecret)
                .build()
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            if (result.containsKey("access_token")) {
                String accessToken = (String) result.get("access_token");
                Integer expiresIn = (Integer) result.get("expires_in");
                long expiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // Buffer 1 minute

                ACCESS_TOKEN_CACHE.put(appId, new TokenCacheEntry(accessToken, expiresAt));
                return accessToken;
            } else {
                throw new RuntimeException("Failed to get access token: " + response);
            }
        } catch (Exception e) {
            log.error("Failed to get access token", e);
            throw new RuntimeException("Failed to get access token", e);
        }
    }

    public record SyncResult(List<WebhookMessageRequest> messages, String nextCursor) {}

    /**
     * 同步微信客服消息
     */
    public SyncResult syncMessages(OfficialChannelConfig config, String token, String cursor) {
        String accessToken = getAccessToken(config);
        String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/sync_msg?access_token=" + accessToken;
        
        List<WebhookMessageRequest> allRequests = new ArrayList<>();
        String nextCursor = cursor;
        boolean hasMore = true;
        
        while (hasMore) {
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("token", token);
            requestBody.put("cursor", nextCursor);
            requestBody.put("limit", 1000);
            
            try {
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody);
                String response = restTemplate.postForObject(url, entity, String.class);
                
                Map<String, Object> result = objectMapper.readValue(response, Map.class);
                
                if ((Integer) result.getOrDefault("errcode", 0) != 0) {
                    log.error("同步消息失败: {}", response);
                    break;
                }
                
                nextCursor = (String) result.get("next_cursor");
                Integer hasMoreInt = (Integer) result.get("has_more");
                hasMore = hasMoreInt != null && hasMoreInt == 1;
                
                List<Map<String, Object>> msgList = (List<Map<String, Object>>) result.get("msg_list");
                if (msgList == null || msgList.isEmpty()) {
                    continue;
                }
                
                for (Map<String, Object> msg : msgList) {
                    // origin: 3-客户发送的消息, 4-系统推送的消息, 5-接待人员在企业微信客户端发送的消息
                    Integer origin = (Integer) msg.get("origin");
                    if (origin == null || origin != 3) {
                        continue;
                    }
                    
                    String msgType = (String) msg.get("msgtype");
                    String externalUserId = (String) msg.get("external_userid");
                    String openKfId = (String) msg.get("open_kfid");
                    String msgId = (String) msg.get("msgid");
                    
                    String content = "";
                    String attachmentUrl = null;
                    String attachmentName = null;
                    
                    if ("text".equals(msgType)) {
                        Map<String, Object> text = (Map<String, Object>) msg.get("text");
                        content = (String) text.get("content");
                    } else if ("image".equals(msgType)) {
                        Map<String, Object> image = (Map<String, Object>) msg.get("image");
                        attachmentUrl = (String) image.get("media_id"); // 需要换取临时素材或永久素材，这里暂时存media_id
                        attachmentName = "image";
                        content = "[图片]";
                    } else if ("voice".equals(msgType)) {
                        content = "[语音]";
                    } else if ("file".equals(msgType)) {
                        content = "[文件]";
                    } else {
                        content = "[不支持的消息类型: " + msgType + "]";
                    }
                    
                    // 构造 Metadata
                    Map<String, Object> metadata = new java.util.HashMap<>();
                    metadata.put("open_kfid", openKfId);
                    metadata.put("msgid", msgId);
                    
                    Long sendTime = ((Number) msg.get("send_time")).longValue() * 1000L;
                    
                    WebhookMessageRequest req = new WebhookMessageRequest(
                            externalUserId, 
                            content,
                            msgType,
                            externalUserId,
                            "微信用户",
                            null,
                            null,
                            null,
                            attachmentUrl,
                            attachmentName,
                            sendTime,
                            null,
                            metadata
                    );
                    
                    allRequests.add(req);
                }
                
            } catch (Exception e) {
                log.error("同步消息异常", e);
                break;
            }
        }
        
        return new SyncResult(allRequests, nextCursor);
    }

    /**
     * 验证微信Webhook签名
     */
    public boolean verifySignature(OfficialChannelConfig config, String signature, 
                                   String timestamp, String nonce) {
        return verifySignature(config, signature, timestamp, nonce, null);
    }

    /**
     * 验证微信Webhook签名 (支持 echostr 加密验证)
     */
    public boolean verifySignature(OfficialChannelConfig config, String signature, 
                                   String timestamp, String nonce, String echostr) {
        if (signature == null || timestamp == null || nonce == null) {
            return false;
        }

        Map<String, Object> configData = channelService.parseConfigJson(config);
        String token = (String) configData.get("token");
        
        if (token == null || token.isBlank()) {
            return false;
        }

        List<String> parts = new ArrayList<>(4);
        parts.add(token);
        parts.add(timestamp);
        parts.add(nonce);
        if (echostr != null) {
            parts.add(echostr);
        }
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
     * 解密 echostr
     */
    public String decryptEchostr(OfficialChannelConfig config, String echostr) {
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String encodingAesKey = (String) configData.get("encodingAESKey");
        
        if (encodingAesKey == null || encodingAesKey.isBlank()) {
            log.warn("未配置 encodingAESKey，无法解密 echostr");
            return echostr; // 如果未配置，原样返回（兼容非加密模式）
        }
        
        return aesUtil.decryptEchostr(encodingAesKey, echostr);
    }

    /**
     * 从 XML Body 中提取 Encrypt 字段
     */
    public String extractEncrypt(String body) {
        try {
            int start = body.indexOf("<Encrypt><![CDATA[");
            if (start == -1) {
                // 尝试不带 CDATA 的格式
                start = body.indexOf("<Encrypt>");
                if (start == -1) return null;
                start += 9;
                int end = body.indexOf("</Encrypt>", start);
                if (end == -1) return null;
                return body.substring(start, end);
            }
            
            start += 18; // length of "<Encrypt><![CDATA["
            int end = body.indexOf("]]></Encrypt>", start);
            if (end == -1) return null;
            return body.substring(start, end);
        } catch (Exception e) {
            log.warn("提取 Encrypt 字段失败", e);
            return null;
        }
    }

    /**
     * 解密消息
     */
    public String decryptMessage(OfficialChannelConfig config, String encrypt) {
        Map<String, Object> configData = channelService.parseConfigJson(config);
        String encodingAesKey = (String) configData.get("encodingAESKey");
        
        if (encodingAesKey == null || encodingAesKey.isBlank()) {
            throw new RuntimeException("未配置 encodingAESKey，无法解密消息");
        }
        
        return aesUtil.decrypt(encodingAesKey, encrypt);
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
     * 发送消息到微信客服（企业微信）
     */
    public void sendKfMessage(OfficialChannelConfig config, String openKfId, String externalUserId, 
                              String content, List<Attachment> attachments) {
        String accessToken = getAccessToken(config);
        String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/send_msg?access_token=" + accessToken;
        
        // 构造消息体
        Map<String, Object> messageMap = new java.util.HashMap<>();
        messageMap.put("touser", externalUserId);
        messageMap.put("open_kfid", openKfId);
        messageMap.put("msgid", java.util.UUID.randomUUID().toString()); // 唯一标识，防重
        
        if (attachments != null && !attachments.isEmpty()) {
             // 暂时只处理第一个附件，或者根据类型判断
             // 微信客服支持 image, voice, video, file, miniprogram, msgmenu, location
             // 这里简单处理，如果有附件且是图片，发送图片，否则发送文本
             Attachment att = attachments.get(0);
             // TODO: 需要上传素材获取 media_id
             // 暂时降级为发送文本链接
             messageMap.put("msgtype", "text");
             messageMap.put("text", Map.of("content", content + "\n[附件]: " + att.getUrl()));
        } else {
            messageMap.put("msgtype", "text");
            messageMap.put("text", Map.of("content", content));
        }
        
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(messageMap);
            String response = restTemplate.postForObject(url, entity, String.class);
            
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            if ((Integer) result.getOrDefault("errcode", 0) != 0) {
                log.error("发送客服消息失败: {}", response);
                throw new RuntimeException("发送客服消息失败: " + result.get("errmsg"));
            }
        } catch (Exception e) {
            log.error("发送客服消息异常", e);
            throw new RuntimeException("发送客服消息异常", e);
        }
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
