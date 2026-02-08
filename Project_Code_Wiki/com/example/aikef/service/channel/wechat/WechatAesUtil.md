# WechatAesUtil

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.wechat`
- **Type**: `Class`
- **Modifiers**: `public`, `Component`, `Slf4j`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 微信 AES 加解密工具类。实现了微信官方的 `WXBizMsgCrypt` 逻辑，用于微信企业号、企业微信和微信客服的消息加解密。

## 2. Method Deep Dive

### `decrypt` / `decryptEchostr`
- **Signature**: `public String decrypt(String encodingAesKey, String cipherText)`
- **Description**: 解密密文。
- **Logic**:
  1. Base64 解码 `EncodingAESKey`。
  2. AES-CBC 解密。
  3. 去除 PKCS7 填充。
  4. 移除 16 字节随机串、4 字节长度头和尾部的 AppId。
  5. 返回明文内容。

### `recoverNetworkBytesOrder`
- **Signature**: `private int recoverNetworkBytesOrder(byte[] orderBytes)`
- **Description**: 还原 4 字节的网络字节序整数（用于获取消息长度）。

## 3. Dependency Graph
- **Dependencies**:
  - `javax.crypto.Cipher`: Java 加密库。

## 4. Usage Guide
### 场景：微信客服回调解密
微信客服的回调消息是加密的 XML。`WechatOfficialAdapter` 调用此工具类，传入配置的 `EncodingAESKey` 和 XML 中的 `Encrypt` 字段，获取解密后的 JSON/XML 字符串。
