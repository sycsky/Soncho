# 第三方平台管理接口文档

## 概述

本文档面向前端开发者，用于在管理后台实现第三方平台配置的增删改查功能。

**基础路径**：`/api/v1/webhook`

---

## 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/platform-types` | 获取平台类型列表 |
| GET | `/auth-types` | 获取认证类型列表 |
| GET | `/platforms` | 获取所有平台配置 |
| GET | `/platforms/{platformName}` | 获取单个平台配置 |
| POST | `/platforms` | 创建平台配置 |
| PUT | `/platforms/{platformId}` | 更新平台配置 |

---

## 枚举值接口

### 获取平台类型列表

用于前端下拉框选项。

**请求**：
```
GET /api/v1/webhook/platform-types
```

**响应**：
```json
[
  { "value": "LINE", "label": "Line" },
  { "value": "WHATSAPP", "label": "WhatsApp" },
  { "value": "WECHAT", "label": "微信" },
  { "value": "TELEGRAM", "label": "Telegram" },
  { "value": "FACEBOOK", "label": "Facebook Messenger" },
  { "value": "WEB", "label": "网页" },
  { "value": "CUSTOM", "label": "自定义平台" }
]
```

---

### 获取认证类型列表

用于前端下拉框选项。

**请求**：
```
GET /api/v1/webhook/auth-types
```

**响应**：
```json
[
  { "value": "NONE", "description": "无认证" },
  { "value": "API_KEY", "description": "API Key (X-API-Key 请求头)" },
  { "value": "BEARER_TOKEN", "description": "Bearer Token" },
  { "value": "BASIC_AUTH", "description": "Basic 认证" },
  { "value": "CUSTOM_HEADER", "description": "自定义请求头" }
]
```

---

## 平台配置 CRUD

### 获取所有平台配置

**请求**：
```
GET /api/v1/webhook/platforms
```

**响应**：
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "line_official",
    "displayName": "Line 官方账号",
    "platformType": "LINE",
    "callbackUrl": "https://api.line.me/v2/bot/message/push",
    "authType": "BEARER_TOKEN",
    "authCredential": "xxx-access-token",
    "extraHeaders": null,
    "webhookSecret": null,
    "enabled": true,
    "remark": "Line 官方账号消息接入",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "name": "wechat_mp",
    "displayName": "微信公众号",
    "platformType": "WECHAT",
    "callbackUrl": "https://your-proxy.com/wechat/send",
    "authType": "API_KEY",
    "authCredential": "your-api-key",
    "extraHeaders": null,
    "webhookSecret": "wechat-token",
    "enabled": true,
    "remark": null,
    "createdAt": "2024-01-16T14:20:00Z",
    "updatedAt": "2024-01-16T14:20:00Z"
  }
]
```

---

### 获取单个平台配置

**请求**：
```
GET /api/v1/webhook/platforms/{platformName}
```

**路径参数**：
- `platformName`：平台名称（如 `line_official`）

**响应成功 (200)**：
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "line_official",
  "displayName": "Line 官方账号",
  "platformType": "LINE",
  "callbackUrl": "https://api.line.me/v2/bot/message/push",
  "authType": "BEARER_TOKEN",
  "authCredential": "xxx-access-token",
  "extraHeaders": null,
  "webhookSecret": null,
  "enabled": true,
  "remark": "Line 官方账号消息接入",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**响应失败 (404)**：
```
Not Found
```

---

### 创建平台配置

**请求**：
```
POST /api/v1/webhook/platforms
Content-Type: application/json
```

**请求体**：
```json
{
  "name": "line_official",
  "displayName": "Line 官方账号",
  "platformType": "LINE",
  "callbackUrl": "https://api.line.me/v2/bot/message/push",
  "authType": "BEARER_TOKEN",
  "authCredential": "your-channel-access-token",
  "webhookSecret": "your-webhook-secret",
  "extraHeaders": "{\"X-Custom-Header\": \"value\"}",
  "enabled": true,
  "remark": "Line 官方账号消息接入"
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | ✅ | 平台唯一标识（英文、数字、下划线） |
| displayName | string | ❌ | 显示名称 |
| platformType | string | ✅ | 平台类型（从 `/platform-types` 获取） |
| callbackUrl | string | ❌ | 回复消息回调 URL |
| authType | string | ❌ | 认证类型（从 `/auth-types` 获取），默认 `NONE` |
| authCredential | string | ❌ | 认证凭据 |
| webhookSecret | string | ❌ | Webhook 签名验证密钥 |
| extraHeaders | string | ❌ | 额外请求头（JSON 字符串格式） |
| enabled | boolean | ❌ | 是否启用，默认 `true` |
| remark | string | ❌ | 备注 |

**响应成功 (200)**：
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "line_official",
  "displayName": "Line 官方账号",
  "platformType": "LINE",
  "callbackUrl": "https://api.line.me/v2/bot/message/push",
  "authType": "BEARER_TOKEN",
  "authCredential": "your-channel-access-token",
  "extraHeaders": "{\"X-Custom-Header\": \"value\"}",
  "webhookSecret": "your-webhook-secret",
  "enabled": true,
  "remark": "Line 官方账号消息接入",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**响应失败 (400)**：
```json
{
  "code": 400,
  "message": "平台名称已存在: line_official"
}
```

---

### 更新平台配置

**请求**：
```
PUT /api/v1/webhook/platforms/{platformId}
Content-Type: application/json
```

**路径参数**：
- `platformId`：平台 ID（UUID 格式）

**请求体**（只传需要更新的字段）：
```json
{
  "displayName": "Line 官方账号 - 更新",
  "callbackUrl": "https://new-url.com/callback",
  "authCredential": "new-access-token",
  "enabled": false
}
```

**可更新字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| displayName | string | 显示名称 |
| callbackUrl | string | 回调 URL |
| authType | string | 认证类型 |
| authCredential | string | 认证凭据 |
| webhookSecret | string | Webhook 密钥 |
| extraHeaders | string | 额外请求头 |
| enabled | boolean | 是否启用 |
| remark | string | 备注 |

> ⚠️ `name` 和 `platformType` 创建后不可修改

**响应成功 (200)**：
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "line_official",
  "displayName": "Line 官方账号 - 更新",
  "platformType": "LINE",
  "callbackUrl": "https://new-url.com/callback",
  "authType": "BEARER_TOKEN",
  "authCredential": "new-access-token",
  "extraHeaders": null,
  "webhookSecret": null,
  "enabled": false,
  "remark": "Line 官方账号消息接入",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T12:00:00Z"
}
```

---

## 前端实现参考

### TypeScript 类型定义

```typescript
// 平台类型选项
interface PlatformTypeOption {
  value: string;
  label: string;
}

// 认证类型选项
interface AuthTypeOption {
  value: string;
  description: string;
}

// 平台配置
interface ExternalPlatform {
  id: string;
  name: string;
  displayName: string | null;
  platformType: string;
  callbackUrl: string | null;
  authType: string;
  authCredential: string | null;
  extraHeaders: string | null;
  webhookSecret: string | null;
  enabled: boolean;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

// 创建平台请求
interface CreatePlatformRequest {
  name: string;
  displayName?: string;
  platformType: string;
  callbackUrl?: string;
  authType?: string;
  authCredential?: string;
  webhookSecret?: string;
  extraHeaders?: string;
  enabled?: boolean;
  remark?: string;
}

// 更新平台请求
interface UpdatePlatformRequest {
  displayName?: string;
  callbackUrl?: string;
  authType?: string;
  authCredential?: string;
  webhookSecret?: string;
  extraHeaders?: string;
  enabled?: boolean;
  remark?: string;
}
```

### API 调用示例

```typescript
const API_BASE = '/api/v1/webhook';

// 获取平台类型列表
async function getPlatformTypes(): Promise<PlatformTypeOption[]> {
  const res = await fetch(`${API_BASE}/platform-types`);
  return res.json();
}

// 获取认证类型列表
async function getAuthTypes(): Promise<AuthTypeOption[]> {
  const res = await fetch(`${API_BASE}/auth-types`);
  return res.json();
}

// 获取所有平台
async function getPlatforms(): Promise<ExternalPlatform[]> {
  const res = await fetch(`${API_BASE}/platforms`);
  return res.json();
}

// 获取单个平台
async function getPlatform(name: string): Promise<ExternalPlatform | null> {
  const res = await fetch(`${API_BASE}/platforms/${name}`);
  if (res.status === 404) return null;
  return res.json();
}

// 创建平台
async function createPlatform(data: CreatePlatformRequest): Promise<ExternalPlatform> {
  const res = await fetch(`${API_BASE}/platforms`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const error = await res.json();
    throw new Error(error.message);
  }
  return res.json();
}

// 更新平台
async function updatePlatform(id: string, data: UpdatePlatformRequest): Promise<ExternalPlatform> {
  const res = await fetch(`${API_BASE}/platforms/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const error = await res.json();
    throw new Error(error.message);
  }
  return res.json();
}
```

### React 表单示例

```tsx
import { useState, useEffect } from 'react';

function PlatformForm({ platform, onSubmit, onCancel }) {
  const [platformTypes, setPlatformTypes] = useState([]);
  const [authTypes, setAuthTypes] = useState([]);
  const [formData, setFormData] = useState({
    name: platform?.name || '',
    displayName: platform?.displayName || '',
    platformType: platform?.platformType || '',
    callbackUrl: platform?.callbackUrl || '',
    authType: platform?.authType || 'NONE',
    authCredential: platform?.authCredential || '',
    webhookSecret: platform?.webhookSecret || '',
    enabled: platform?.enabled ?? true,
    remark: platform?.remark || ''
  });

  useEffect(() => {
    // 加载下拉选项
    getPlatformTypes().then(setPlatformTypes);
    getAuthTypes().then(setAuthTypes);
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* 平台名称 - 创建后不可修改 */}
      <div>
        <label>平台名称 *</label>
        <input
          type="text"
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          disabled={!!platform}
          required
          placeholder="英文标识，如: line_official"
        />
      </div>

      {/* 显示名称 */}
      <div>
        <label>显示名称</label>
        <input
          type="text"
          value={formData.displayName}
          onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
          placeholder="Line 官方账号"
        />
      </div>

      {/* 平台类型 - 创建后不可修改 */}
      <div>
        <label>平台类型 *</label>
        <select
          value={formData.platformType}
          onChange={(e) => setFormData({ ...formData, platformType: e.target.value })}
          disabled={!!platform}
          required
        >
          <option value="">请选择</option>
          {platformTypes.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
      </div>

      {/* 回调 URL */}
      <div>
        <label>回调 URL</label>
        <input
          type="url"
          value={formData.callbackUrl}
          onChange={(e) => setFormData({ ...formData, callbackUrl: e.target.value })}
          placeholder="https://api.example.com/callback"
        />
        <small>系统回复消息时会 POST 到此地址</small>
      </div>

      {/* 认证类型 */}
      <div>
        <label>认证类型</label>
        <select
          value={formData.authType}
          onChange={(e) => setFormData({ ...formData, authType: e.target.value })}
        >
          {authTypes.map((type) => (
            <option key={type.value} value={type.value}>
              {type.description}
            </option>
          ))}
        </select>
      </div>

      {/* 认证凭据 */}
      {formData.authType !== 'NONE' && (
        <div>
          <label>认证凭据</label>
          <input
            type="password"
            value={formData.authCredential}
            onChange={(e) => setFormData({ ...formData, authCredential: e.target.value })}
            placeholder={getCredentialPlaceholder(formData.authType)}
          />
        </div>
      )}

      {/* Webhook 密钥 */}
      <div>
        <label>Webhook 密钥</label>
        <input
          type="password"
          value={formData.webhookSecret}
          onChange={(e) => setFormData({ ...formData, webhookSecret: e.target.value })}
          placeholder="用于验证来自第三方平台的请求"
        />
      </div>

      {/* 是否启用 */}
      <div>
        <label>
          <input
            type="checkbox"
            checked={formData.enabled}
            onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
          />
          启用
        </label>
      </div>

      {/* 备注 */}
      <div>
        <label>备注</label>
        <textarea
          value={formData.remark}
          onChange={(e) => setFormData({ ...formData, remark: e.target.value })}
          rows={3}
        />
      </div>

      <div>
        <button type="submit">{platform ? '保存' : '创建'}</button>
        <button type="button" onClick={onCancel}>取消</button>
      </div>
    </form>
  );
}

function getCredentialPlaceholder(authType) {
  switch (authType) {
    case 'API_KEY': return 'your-api-key';
    case 'BEARER_TOKEN': return 'your-access-token';
    case 'BASIC_AUTH': return 'username:password';
    case 'CUSTOM_HEADER': return 'Header-Name:value';
    default: return '';
  }
}
```

---

## UI 界面建议

### 列表页

```
┌─────────────────────────────────────────────────────────────────────┐
│  第三方平台管理                                    [+ 新建平台]      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 名称           │ 平台类型 │ 回调URL      │ 状态  │ 操作    │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ line_official  │ Line    │ https://...  │ ✅启用 │ 编辑    │   │
│  │ wechat_mp      │ 微信    │ https://...  │ ✅启用 │ 编辑    │   │
│  │ whatsapp_biz   │ WhatsApp│ -            │ ⚪禁用 │ 编辑    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 新建/编辑弹窗

```
┌─────────────────────────────────────────────────────┐
│  新建平台配置                                    ✕  │
├─────────────────────────────────────────────────────┤
│                                                     │
│  平台名称 *        [line_official        ]          │
│                    英文标识，创建后不可修改          │
│                                                     │
│  显示名称          [Line 官方账号        ]          │
│                                                     │
│  平台类型 *        [Line              ▼]           │
│                    创建后不可修改                   │
│                                                     │
│  回调 URL          [https://...         ]          │
│                    系统回复消息时会 POST 到此地址    │
│                                                     │
│  认证类型          [Bearer Token      ▼]           │
│                                                     │
│  认证凭据          [••••••••••••••••••  ]          │
│                                                     │
│  Webhook 密钥      [••••••••••••••••••  ]          │
│                    用于验证第三方平台请求            │
│                                                     │
│  [✓] 启用                                          │
│                                                     │
│  备注                                               │
│  ┌───────────────────────────────────────────┐     │
│  │ Line 官方账号消息接入                      │     │
│  └───────────────────────────────────────────┘     │
│                                                     │
│                          [取消]  [保存]            │
└─────────────────────────────────────────────────────┘
```

---

## 注意事项

1. **name 字段规范**
   - 只能包含英文字母、数字、下划线
   - 创建后不可修改
   - 用于 Webhook URL 路径：`/api/v1/webhook/{name}/message`

2. **platformType 不可修改**
   - 创建后不可更改平台类型
   - 如需更换平台类型，请新建配置

3. **敏感信息**
   - `authCredential` 和 `webhookSecret` 是敏感信息
   - 前端显示时建议使用 `type="password"`
   - 更新时如果不传，会保留原值

4. **extraHeaders 格式**
   - JSON 字符串格式
   - 示例：`"{\"X-Custom\": \"value\", \"X-Another\": \"value2\"}"`

