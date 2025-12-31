# 官方渠道配置表单字段说明

本文档定义了前端在配置各官方渠道时所需的表单字段。所有特定平台的配置数据都应以 JSON 格式存储在 `configData` 字段中。

## 1. 微信系平台

### 微信服务号 (WECHAT_OFFICIAL)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `appId` | 是 | String | 微信开发者ID (AppID) |
| `appSecret` | 是 | String | 开发者密码 (AppSecret) |
| `token` | 是 | String | 令牌 (Token)，用于Webhook验证 |
| `encodingAESKey` | 否 | String | 消息加解密密钥 (EncodingAESKey) |

### 微信客服 (WECHAT_KF)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `appId` | 是 | String | 企业ID (CorpID) - **注意：后端复用 appId 字段存储 CorpID** |
| `appSecret` | 是 | String | 客服应用Secret |
| `token` | 是 | String | 令牌 (Token) |
| `encodingAESKey` | 否 | String | 消息加解密密钥 |

---

## 2. 海外社交平台

### Facebook Messenger (FACEBOOK_MESSENGER)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `appId` | 否 | String | Facebook App ID |
| `appSecret` | 否 | String | Facebook App Secret |
| `accessToken` | 是 | String | Page Access Token (长期令牌) |

> **注意**：Webhook 验证 Token 请填入公共字段 `webhookSecret`。

### Instagram Direct (INSTAGRAM)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `appId` | 否 | String | Facebook App ID |
| `appSecret` | 否 | String | Facebook App Secret |
| `accessToken` | 是 | String | Page Access Token (长期令牌) |

> **注意**：Instagram Messaging API 依赖于 Facebook Graph API，配置方式与 Facebook Messenger 类似。

### Telegram (TELEGRAM)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `botToken` | 是 | String | Telegram Bot Token (例如: `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`) |

### WhatsApp Business (WHATSAPP_OFFICIAL)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `phoneNumberId` | 是 | String | 电话号码 ID |
| `accessToken` | 是 | String | System User Access Token |
| `businessAccountId` | 否 | String | WhatsApp Business Account ID |
| `appId` | 否 | String | Meta App ID |
| `appSecret` | 否 | String | Meta App Secret |

### Line (LINE_OFFICIAL)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `channelId` | 是 | String | Channel ID |
| `channelSecret` | 是 | String | Channel Secret |
| `channelAccessToken` | 是 | String | Channel Access Token (Long-lived) |

### X (Twitter) (TWITTER)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `consumerKey` | 是 | String | API Key (Consumer Key) |
| `consumerSecret` | 是 | String | API Key Secret (Consumer Secret) |
| `accessToken` | 是 | String | Access Token |
| `accessTokenSecret` | 是 | String | Access Token Secret |
| `bearerToken` | 否 | String | Bearer Token (API v2) |

---

## 3. 国内其他平台

### 抖音 (DOUYIN)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `clientKey` | 是 | String | 应用 Client Key (App Key) |
| `clientSecret` | 是 | String | 应用 Client Secret |

### 小红书 (RED_BOOK)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `appId` | 是 | String | 应用 App ID |
| `appSecret` | 是 | String | 应用 App Secret |

### 微博 (WEIBO)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `appKey` | 是 | String | App Key |
| `appSecret` | 是 | String | App Secret |
| `accessToken` | 是 | String | 访问令牌 |

---

## 4. 邮件渠道

### Email (EMAIL)
| 字段名 | 必填 | 类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `smtpHost` | 是 | String | SMTP 服务器地址 (如 smtp.gmail.com) |
| `smtpPort` | 是 | Number | SMTP 端口 (如 587 或 465) |
| `username` | 是 | String | 邮箱账号 |
| `password` | 是 | String | 邮箱密码/应用专用密码 |
| `fromEmail` | 是 | String | 发件人邮箱地址 |
| `sslEnabled` | 否 | Boolean | 是否启用 SSL/TLS |

---

## 公共字段说明

除了上述 `configData` 中的特定字段外，所有渠道配置都需要填写以下公共字段：

| 字段名 | 必填 | 说明 |
| :--- | :--- | :--- |
| `displayName` | 否 | 渠道显示名称（方便后台管理区分） |
| `webhookSecret` | 否 | Webhook 验证密钥（部分平台如 Facebook/微信 需要填写此字段作为 Verify Token） |
| `enabled` | 是 | 是否启用该渠道 |
| `remark` | 否 | 备注信息 |
