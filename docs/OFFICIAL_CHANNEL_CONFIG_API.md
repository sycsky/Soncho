# 官方渠道配置 API 文档

## 概述

官方渠道配置用于管理微信服务号、Line官方账号、WhatsApp Business等官方平台的接入配置。每个平台只有一个配置记录（存在则更新，不存在则创建）。

## 基础信息

| 项目 | 说明 |
|------|------|
| 基础路径 | `/api/v1/official-channels` |
| 认证方式 | Bearer Token（需要登录）|
| Content-Type | `application/json` |

---

## API 接口

### 1. 获取所有官方渠道配置

**请求**

```
GET /api/v1/official-channels/configs
Authorization: Bearer <token>
```

**响应**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "channelType": "WECHAT_OFFICIAL",
    "displayName": "微信服务号",
    "enabled": true,
    "configJson": "{\"appId\":\"wx1234567890abcdef\",\"appSecret\":\"secret123456\"}",
    "webhookSecret": "your_webhook_secret",
    "webhookUrl": "/api/v1/official-channels/wechat_official/webhook",
    "remark": "微信服务号配置",
    "createdAt": "2024-12-15T10:30:00Z",
    "updatedAt": "2024-12-15T10:30:00Z"
  }
]
```

---

### 2. 获取指定渠道配置

**请求**

```
GET /api/v1/official-channels/configs/{channelType}
Authorization: Bearer <token>
```

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `channelType` | String | 渠道类型：`WECHAT_OFFICIAL`、`LINE_OFFICIAL`、`WHATSAPP_OFFICIAL` |

**响应**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "channelType": "WECHAT_OFFICIAL",
  "displayName": "微信服务号",
  "enabled": true,
  "configJson": "{\"appId\":\"wx1234567890abcdef\",\"appSecret\":\"secret123456\"}",
  "webhookSecret": "your_webhook_secret",
  "webhookUrl": "/api/v1/official-channels/wechat_official/webhook",
  "remark": "微信服务号配置",
  "createdAt": "2024-12-15T10:30:00Z",
  "updatedAt": "2024-12-15T10:30:00Z"
}
```

---

### 3. 保存或更新官方渠道配置

**请求**

```
POST /api/v1/official-channels/configs
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**

```json
{
  "channelType": "WECHAT_OFFICIAL",
  "configData": {
    "appId": "wx1234567890abcdef",
    "appSecret": "secret123456",
    "token": "your_token",
    "encodingAESKey": "your_encoding_key"
  },
  "webhookSecret": "your_webhook_secret",
  "enabled": true,
  "remark": "微信服务号配置"
}
```

**参数说明**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `channelType` | String | ✅ | 渠道类型 |
| `configData` | Object | ✅ | 平台特定配置（见下方各平台配置说明）|
| `webhookSecret` | String | ❌ | Webhook验证密钥 |
| `enabled` | Boolean | ❌ | 是否启用，默认 `false` |
| `remark` | String | ❌ | 备注 |

**响应**

返回保存后的配置对象（格式同获取接口）。

---

### 4. 启用/禁用官方渠道

**请求**

```
PATCH /api/v1/official-channels/configs/{channelType}/toggle?enabled=true
Authorization: Bearer <token>
```

**响应**

返回更新后的配置对象。

---

### 5. 删除官方渠道配置

**请求**

```
DELETE /api/v1/official-channels/configs/{channelType}
Authorization: Bearer <token>
```

**响应**

- 成功：`204 No Content`

---

### 6. 获取所有渠道类型

**请求**

```
GET /api/v1/official-channels/channel-types
Authorization: Bearer <token>
```

**响应**

```json
[
  {
    "value": "WECHAT_OFFICIAL",
    "label": "微信服务号"
  },
  {
    "value": "LINE_OFFICIAL",
    "label": "Line官方账号"
  },
  {
    "value": "WHATSAPP_OFFICIAL",
    "label": "WhatsApp Business"
  }
]
```

---

## 各平台配置说明

### 微信服务号 (WECHAT_OFFICIAL)

**configData 字段**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `appId` | String | ✅ | 微信公众号 AppID |
| `appSecret` | String | ✅ | 微信公众号 AppSecret |
| `token` | String | ✅ | 消息验证 Token |
| `encodingAESKey` | String | ❌ | 消息加解密密钥（加密模式需要）|

**配置示例**

```json
{
  "channelType": "WECHAT_OFFICIAL",
  "configData": {
    "appId": "wx1234567890abcdef",
    "appSecret": "secret1234567890abcdef1234567890abcdef",
    "token": "your_custom_token",
    "encodingAESKey": "your_encoding_aes_key_43_chars"
  },
  "webhookSecret": "your_webhook_secret",
  "enabled": true,
  "remark": "微信服务号配置"
}
```

**Webhook URL**

```
https://your-domain.com/api/v1/official-channels/wechat_official/webhook
```

**在微信公众平台配置**

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入 **开发** → **基本配置**
3. 设置 **服务器配置**：
   - **URL**: `https://your-domain.com/api/v1/official-channels/wechat_official/webhook`
   - **Token**: 填写配置中的 `token`
   - **EncodingAESKey**: 填写配置中的 `encodingAESKey`（如果使用加密模式）
   - **消息加解密方式**: 选择 **兼容模式** 或 **安全模式**

---

### Line官方账号 (LINE_OFFICIAL)

**configData 字段**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `channelId` | String | ✅ | Line Channel ID |
| `channelSecret` | String | ✅ | Line Channel Secret |
| `channelAccessToken` | String | ✅ | Line Channel Access Token |

**配置示例**

```json
{
  "channelType": "LINE_OFFICIAL",
  "configData": {
    "channelId": "1234567890",
    "channelSecret": "secret1234567890abcdef1234567890abcdef",
    "channelAccessToken": "your_channel_access_token_here"
  },
  "webhookSecret": "your_webhook_secret",
  "enabled": true,
  "remark": "Line官方账号配置"
}
```

**Webhook URL**

```
https://your-domain.com/api/v1/official-channels/line_official/webhook
```

**在Line Developers配置**

1. 登录 [Line Developers Console](https://developers.line.biz/console/)
2. 选择你的 Provider 和 Channel
3. 进入 **Messaging API** 标签页
4. 设置 **Webhook URL**: `https://your-domain.com/api/v1/official-channels/line_official/webhook`
5. 启用 **Use webhook**

---

### WhatsApp Business (WHATSAPP_OFFICIAL)

**configData 字段**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phoneNumberId` | String | ✅ | WhatsApp 电话号码ID |
| `accessToken` | String | ✅ | WhatsApp Access Token |
| `businessAccountId` | String | ❌ | WhatsApp Business Account ID |
| `appId` | String | ❌ | WhatsApp App ID |
| `appSecret` | String | ❌ | WhatsApp App Secret（用于验证Webhook签名）|

**配置示例**

```json
{
  "channelType": "WHATSAPP_OFFICIAL",
  "configData": {
    "phoneNumberId": "123456789012345",
    "accessToken": "your_whatsapp_access_token",
    "businessAccountId": "123456789012345",
    "appId": "your_app_id",
    "appSecret": "your_app_secret"
  },
  "webhookSecret": "your_webhook_secret",
  "enabled": true,
  "remark": "WhatsApp Business配置"
}
```

**Webhook URL**

```
https://your-domain.com/api/v1/official-channels/whatsapp_official/webhook
```

**在Meta for Developers配置**

1. 登录 [Meta for Developers](https://developers.facebook.com/)
2. 选择你的 App
3. 进入 **WhatsApp** → **Configuration**
4. 设置 **Webhook URL**: `https://your-domain.com/api/v1/official-channels/whatsapp_official/webhook`
5. 设置 **Verify Token**: 填写配置中的 `webhookSecret`
6. 订阅 **messages** 事件

---

## 前端使用示例

### React 组件示例

```tsx
import { useState, useEffect } from 'react';
import axios from 'axios';

interface OfficialChannelConfig {
  id: string;
  channelType: 'WECHAT_OFFICIAL' | 'LINE_OFFICIAL' | 'WHATSAPP_OFFICIAL';
  displayName: string;
  enabled: boolean;
  configJson: string;
  webhookSecret?: string;
  webhookUrl: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

interface ChannelTypeInfo {
  value: string;
  label: string;
}

function OfficialChannelConfigPage() {
  const [configs, setConfigs] = useState<OfficialChannelConfig[]>([]);
  const [channelTypes, setChannelTypes] = useState<ChannelTypeInfo[]>([]);
  const [loading, setLoading] = useState(false);

  // 获取所有配置
  useEffect(() => {
    loadConfigs();
    loadChannelTypes();
  }, []);

  const loadConfigs = async () => {
    try {
      const response = await axios.get('/api/v1/official-channels/configs', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setConfigs(response.data);
    } catch (error) {
      console.error('加载配置失败', error);
    }
  };

  const loadChannelTypes = async () => {
    try {
      const response = await axios.get('/api/v1/official-channels/channel-types', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setChannelTypes(response.data);
    } catch (error) {
      console.error('加载渠道类型失败', error);
    }
  };

  // 保存或更新配置
  const saveConfig = async (channelType: string, configData: any) => {
    setLoading(true);
    try {
      await axios.post('/api/v1/official-channels/configs', {
        channelType,
        configData,
        webhookSecret: configData.webhookSecret || '',
        enabled: configData.enabled || false,
        remark: configData.remark || ''
      }, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      await loadConfigs();
      alert('配置保存成功');
    } catch (error) {
      console.error('保存配置失败', error);
      alert('保存失败: ' + (error as any).response?.data?.message || '未知错误');
    } finally {
      setLoading(false);
    }
  };

  // 启用/禁用
  const toggleChannel = async (channelType: string, enabled: boolean) => {
    try {
      await axios.patch(
        `/api/v1/official-channels/configs/${channelType}/toggle?enabled=${enabled}`,
        {},
        { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
      );
      await loadConfigs();
    } catch (error) {
      console.error('切换状态失败', error);
    }
  };

  // 删除配置
  const deleteConfig = async (channelType: string) => {
    if (!confirm('确定要删除此配置吗？')) return;
    
    try {
      await axios.delete(`/api/v1/official-channels/configs/${channelType}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      await loadConfigs();
      alert('配置已删除');
    } catch (error) {
      console.error('删除配置失败', error);
    }
  };

  return (
    <div className="official-channel-config">
      <h2>官方渠道配置</h2>
      
      {channelTypes.map(type => {
        const config = configs.find(c => c.channelType === type.value);
        return (
          <ChannelConfigCard
            key={type.value}
            channelType={type}
            config={config}
            onSave={saveConfig}
            onToggle={toggleChannel}
            onDelete={deleteConfig}
            loading={loading}
          />
        );
      })}
    </div>
  );
}

// 配置卡片组件
function ChannelConfigCard({
  channelType,
  config,
  onSave,
  onToggle,
  onDelete,
  loading
}: {
  channelType: ChannelTypeInfo;
  config?: OfficialChannelConfig;
  onSave: (type: string, data: any) => void;
  onToggle: (type: string, enabled: boolean) => void;
  onDelete: (type: string) => void;
  loading: boolean;
}) {
  const [formData, setFormData] = useState<any>(() => {
    if (config?.configJson) {
      try {
        return JSON.parse(config.configJson);
      } catch {
        return {};
      }
    }
    return getDefaultConfig(channelType.value);
  });

  const [enabled, setEnabled] = useState(config?.enabled || false);
  const [webhookSecret, setWebhookSecret] = useState(config?.webhookSecret || '');
  const [remark, setRemark] = useState(config?.remark || '');

  const getDefaultConfig = (type: string) => {
    switch (type) {
      case 'WECHAT_OFFICIAL':
        return { appId: '', appSecret: '', token: '', encodingAESKey: '' };
      case 'LINE_OFFICIAL':
        return { channelId: '', channelSecret: '', channelAccessToken: '' };
      case 'WHATSAPP_OFFICIAL':
        return { phoneNumberId: '', accessToken: '', businessAccountId: '', appId: '', appSecret: '' };
      default:
        return {};
    }
  };

  const handleSave = () => {
    onSave(channelType.value, {
      ...formData,
      webhookSecret,
      enabled,
      remark
    });
  };

  const handleToggle = (checked: boolean) => {
    setEnabled(checked);
    onToggle(channelType.value, checked);
  };

  return (
    <div className="config-card">
      <div className="card-header">
        <h3>{channelType.label}</h3>
        <div className="actions">
          <label>
            <input
              type="checkbox"
              checked={enabled}
              onChange={(e) => handleToggle(e.target.checked)}
            />
            启用
          </label>
          {config && (
            <button onClick={() => onDelete(channelType.value)}>
              删除
            </button>
          )}
        </div>
      </div>

      <div className="card-body">
        {/* 微信服务号配置表单 */}
        {channelType.value === 'WECHAT_OFFICIAL' && (
          <div className="form-group">
            <label>AppID *</label>
            <input
              value={formData.appId || ''}
              onChange={(e) => setFormData({ ...formData, appId: e.target.value })}
              placeholder="wx1234567890abcdef"
            />
            <label>AppSecret *</label>
            <input
              type="password"
              value={formData.appSecret || ''}
              onChange={(e) => setFormData({ ...formData, appSecret: e.target.value })}
              placeholder="微信公众号AppSecret"
            />
            <label>Token *</label>
            <input
              value={formData.token || ''}
              onChange={(e) => setFormData({ ...formData, token: e.target.value })}
              placeholder="消息验证Token"
            />
            <label>EncodingAESKey</label>
            <input
              value={formData.encodingAESKey || ''}
              onChange={(e) => setFormData({ ...formData, encodingAESKey: e.target.value })}
              placeholder="消息加解密密钥（43字符）"
            />
          </div>
        )}

        {/* Line配置表单 */}
        {channelType.value === 'LINE_OFFICIAL' && (
          <div className="form-group">
            <label>Channel ID *</label>
            <input
              value={formData.channelId || ''}
              onChange={(e) => setFormData({ ...formData, channelId: e.target.value })}
              placeholder="1234567890"
            />
            <label>Channel Secret *</label>
            <input
              type="password"
              value={formData.channelSecret || ''}
              onChange={(e) => setFormData({ ...formData, channelSecret: e.target.value })}
              placeholder="Line Channel Secret"
            />
            <label>Channel Access Token *</label>
            <input
              type="password"
              value={formData.channelAccessToken || ''}
              onChange={(e) => setFormData({ ...formData, channelAccessToken: e.target.value })}
              placeholder="Line Channel Access Token"
            />
          </div>
        )}

        {/* WhatsApp配置表单 */}
        {channelType.value === 'WHATSAPP_OFFICIAL' && (
          <div className="form-group">
            <label>Phone Number ID *</label>
            <input
              value={formData.phoneNumberId || ''}
              onChange={(e) => setFormData({ ...formData, phoneNumberId: e.target.value })}
              placeholder="123456789012345"
            />
            <label>Access Token *</label>
            <input
              type="password"
              value={formData.accessToken || ''}
              onChange={(e) => setFormData({ ...formData, accessToken: e.target.value })}
              placeholder="WhatsApp Access Token"
            />
            <label>Business Account ID</label>
            <input
              value={formData.businessAccountId || ''}
              onChange={(e) => setFormData({ ...formData, businessAccountId: e.target.value })}
              placeholder="123456789012345"
            />
            <label>App ID</label>
            <input
              value={formData.appId || ''}
              onChange={(e) => setFormData({ ...formData, appId: e.target.value })}
              placeholder="WhatsApp App ID"
            />
            <label>App Secret</label>
            <input
              type="password"
              value={formData.appSecret || ''}
              onChange={(e) => setFormData({ ...formData, appSecret: e.target.value })}
              placeholder="WhatsApp App Secret"
            />
          </div>
        )}

        <div className="form-group">
          <label>Webhook Secret</label>
          <input
            type="password"
            value={webhookSecret}
            onChange={(e) => setWebhookSecret(e.target.value)}
            placeholder="Webhook验证密钥（可选）"
          />
        </div>

        <div className="form-group">
          <label>备注</label>
          <textarea
            value={remark}
            onChange={(e) => setRemark(e.target.value)}
            placeholder="配置备注"
            rows={3}
          />
        </div>

        {config?.webhookUrl && (
          <div className="webhook-url">
            <label>Webhook URL</label>
            <div className="url-display">
              <code>{window.location.origin}{config.webhookUrl}</code>
              <button
                onClick={() => navigator.clipboard.writeText(window.location.origin + config.webhookUrl)}
              >
                复制
              </button>
            </div>
            <small>将此URL配置到对应平台的Webhook设置中</small>
          </div>
        )}

        <button
          onClick={handleSave}
          disabled={loading}
          className="save-button"
        >
          {loading ? '保存中...' : config ? '更新配置' : '保存配置'}
        </button>
      </div>
    </div>
  );
}

export default OfficialChannelConfigPage;
```

---

## 数据结构

### OfficialChannelConfig

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 配置ID |
| `channelType` | String | 渠道类型（`WECHAT_OFFICIAL`、`LINE_OFFICIAL`、`WHATSAPP_OFFICIAL`）|
| `displayName` | String | 渠道显示名称 |
| `enabled` | Boolean | 是否启用 |
| `configJson` | String | 配置信息（JSON字符串）|
| `webhookSecret` | String | Webhook验证密钥 |
| `webhookUrl` | String | Webhook URL（系统自动生成）|
| `remark` | String | 备注 |
| `createdAt` | ISO DateTime | 创建时间 |
| `updatedAt` | ISO DateTime | 更新时间 |

---

## 注意事项

1. **唯一性**：每个渠道类型只能有一个配置记录，保存时会自动更新已存在的配置
2. **Webhook URL**：系统会自动生成Webhook URL，格式为 `/api/v1/official-channels/{channelType}/webhook`
3. **配置安全**：`configData` 中的敏感信息（如 `appSecret`、`accessToken`）应加密存储
4. **启用状态**：只有 `enabled=true` 的配置才会接收和处理Webhook消息
5. **配置验证**：各平台的配置字段需要根据官方文档填写，系统不会验证配置的有效性

---

## 错误响应

### 配置不存在

```json
{
  "status": 404,
  "error": "Not Found"
}
```

### 配置保存失败

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "配置数据格式错误"
}
```

---

**文档版本**: v1.0  
**最后更新**: 2024-12-15  
**维护团队**: AI 开发组

