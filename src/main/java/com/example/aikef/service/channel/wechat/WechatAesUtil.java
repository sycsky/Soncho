package com.example.aikef.service.channel.wechat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信 AES 加解密工具类
 * 
 * 用于微信企业号/企业微信/微信客服的消息加解密
 * 参考官方 WXBizMsgCrypt 实现
 */
@Slf4j
@Component
public class WechatAesUtil {

    /**
     * 解密密文 (适用于 echostr 和 消息体)
     * 
     * @param encodingAesKey 配置的 EncodingAESKey
     * @param cipherText 密文 (echostr 或 xml中的Encrypt字段)
     * @return 解密后的明文
     */
    public String decrypt(String encodingAesKey, String cipherText) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
            byte[] original = decrypt(aesKey, Base64.getDecoder().decode(cipherText));
            
            // 去除补位字符
            byte[] bytes = PKCS7Encoder.decode(original);
            
            // 分离16位随机字符串, networkBytesOrder和receiveId
            byte[] networkOrder = Arrays.copyOfRange(bytes, 16, 20);
            int xmlLength = recoverNetworkBytesOrder(networkOrder);
            
            String fromCorpid = new String(Arrays.copyOfRange(bytes, 20 + xmlLength, bytes.length), StandardCharsets.UTF_8);
            String content = new String(Arrays.copyOfRange(bytes, 20, 20 + xmlLength), StandardCharsets.UTF_8);
            
            log.debug("解密成功: content={}, fromCorpid={}", content, fromCorpid);
            return content;
            
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 解密 echostr (兼容旧方法名)
     */
    public String decryptEchostr(String encodingAesKey, String echostr) {
        return decrypt(encodingAesKey, echostr);
    }

    /**
     * AES 解密
     */
    private byte[] decrypt(byte[] aesKey, byte[] encrypted) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
        return cipher.doFinal(encrypted);
    }

    /**
     * 还原4个字节的网络字节序
     */
    private int recoverNetworkBytesOrder(byte[] orderBytes) {
        int sourceNumber = 0;
        for (int i = 0; i < 4; i++) {
            sourceNumber <<= 8;
            sourceNumber |= orderBytes[i] & 0xff;
        }
        return sourceNumber;
    }

    /**
     * PKCS7 补位处理
     */
    private static class PKCS7Encoder {
        static byte[] decode(byte[] decrypted) {
            int pad = decrypted[decrypted.length - 1];
            if (pad < 1 || pad > 32) {
                pad = 0;
            }
            return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
        }
    }
}
