package com.example.aikef.controller;

import com.example.aikef.model.OfficialChannelConfig;
import com.example.aikef.service.OfficialChannelMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 官方渠道Webhook接收控制器
 * 接收来自微信服务号、Line、WhatsApp等官方平台的消息
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/official-channels")
@RequiredArgsConstructor
public class OfficialChannelWebhookController {

    private final OfficialChannelMessageService messageService;

    /**
     * 接收微信服务号Webhook消息
     * 
     * URL: POST /api/v1/official-channels/wechat_official/webhook
     * 
     * 微信验证请求（GET）:
     * GET /api/v1/official-channels/wechat_official/webhook?signature=xxx&timestamp=xxx&nonce=xxx&echostr=xxx
     */
    @RequestMapping(value = "/wechat_official/webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> wechatWebhook(
            @RequestParam(required = false) String signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce,
            @RequestParam(required = false) String echostr,
            @RequestBody(required = false) String body) {
        
        log.info("收到微信服务号Webhook: method={}, signature={}, echostr={}", 
                body != null ? "POST" : "GET", signature, echostr);
        
        // GET请求：微信验证
        if (echostr != null) {
            return messageService.verifyWechatWebhook(signature, timestamp, nonce, echostr);
        }
        
        // POST请求：接收消息
        if (body != null) {
            return messageService.handleWechatMessage(body, signature, timestamp, nonce);
        }
        
        return ResponseEntity.ok("OK");
    }

    @RequestMapping(value = "/wechat_kf/webhook", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> wechatKfWebhook(
            @RequestParam(required = false, name = "msg_signature") String msgSignature,
            @RequestParam(required = false) String signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce,
            @RequestParam(required = false) String echostr,
            @RequestBody(required = false) String body) {

        String effectiveSignature = msgSignature != null ? msgSignature : signature;

        log.info("收到微信客服Webhook: method={}, signature={}, msg_signature={}, echostr={}",
                body != null ? "POST" : "GET", signature, msgSignature, echostr);

        if (echostr != null) {
            return messageService.verifyWechatKfWebhook(effectiveSignature, timestamp, nonce, echostr);
        }

        if (body != null) {
            return messageService.handleWechatKfMessage(body, effectiveSignature, timestamp, nonce);
        }

        return ResponseEntity.ok("OK");
    }

    /**
     * 接收Line官方账号Webhook消息
     * 
     * URL: POST /api/v1/official-channels/line_official/webhook
     */
    @PostMapping("/line_official/webhook")
    public ResponseEntity<Map<String, Object>> lineWebhook(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String body) {
        
        log.info("收到Line官方账号Webhook: signature={}", signature);
        
        return messageService.handleLineMessage(body, signature);
    }

    /**
     * 接收WhatsApp Business Webhook消息
     * 
     * URL: POST /api/v1/official-channels/whatsapp_official/webhook
     */
    @PostMapping("/whatsapp_official/webhook")
    public ResponseEntity<Map<String, Object>> whatsappWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String body) {
        
        log.info("收到WhatsApp Business Webhook: signature={}", signature);
        
        return messageService.handleWhatsappMessage(body, signature);
    }

    // ==================== New Platforms ====================

    // Facebook Messenger
    @RequestMapping(value = "/facebook_messenger/webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> facebookWebhook(
            @RequestParam(required = false, name = "hub.mode") String mode,
            @RequestParam(required = false, name = "hub.verify_token") String token,
            @RequestParam(required = false, name = "hub.challenge") String challenge,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) String body) {
        
        if (challenge != null) {
            return messageService.verifyFacebookWebhook(OfficialChannelConfig.ChannelType.FACEBOOK_MESSENGER, mode, token, challenge);
        }
        if (body != null) {
            return messageService.handleFacebookMessage(OfficialChannelConfig.ChannelType.FACEBOOK_MESSENGER, body, signature);
        }
        return ResponseEntity.ok("OK");
    }

    // Instagram (same logic as Facebook)
    @RequestMapping(value = "/instagram/webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> instagramWebhook(
            @RequestParam(required = false, name = "hub.mode") String mode,
            @RequestParam(required = false, name = "hub.verify_token") String token,
            @RequestParam(required = false, name = "hub.challenge") String challenge,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) String body) {
        
        if (challenge != null) {
            return messageService.verifyFacebookWebhook(OfficialChannelConfig.ChannelType.INSTAGRAM, mode, token, challenge);
        }
        if (body != null) {
            return messageService.handleFacebookMessage(OfficialChannelConfig.ChannelType.INSTAGRAM, body, signature);
        }
        return ResponseEntity.ok("OK");
    }

    // Telegram
    @PostMapping("/telegram/webhook")
    public ResponseEntity<String> telegramWebhook(@RequestBody String body) {
        return messageService.handleTelegramMessage(body);
    }

    // Twitter (CRC & Webhook)
    @RequestMapping(value = "/twitter/webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> twitterWebhook(
            @RequestParam(required = false, name = "crc_token") String crcToken,
            @RequestBody(required = false) String body) {
        
        if (crcToken != null) {
            return messageService.verifyTwitterCrc(crcToken);
        }
        if (body != null) {
            return messageService.handleTwitterMessage(body);
        }
        return ResponseEntity.ok("OK");
    }

    // Douyin
    @PostMapping("/douyin/webhook")
    public ResponseEntity<String> douyinWebhook(
            @RequestHeader(value = "X-Douyin-Signature", required = false) String signature,
            @RequestBody String body) {
        return messageService.handleDouyinMessage(body, signature);
    }

    // RedBook
    @PostMapping("/red_book/webhook")
    public ResponseEntity<String> redBookWebhook(
            @RequestHeader(value = "X-RedBook-Signature", required = false) String signature,
            @RequestBody String body) {
        return messageService.handleRedBookMessage(body, signature);
    }

    // Weibo
    @PostMapping("/weibo/webhook")
    public ResponseEntity<String> weiboWebhook(
            @RequestHeader(value = "X-Weibo-Signature", required = false) String signature,
            @RequestBody String body) {
        return messageService.handleWeiboMessage(body, signature);
    }

    // Email
    @PostMapping("/email/webhook")
    public ResponseEntity<String> emailWebhook(@RequestBody String body) {
        return messageService.handleEmailMessage(body);
    }
}

