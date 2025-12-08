# 客户模块 - 快速开始

## 📋 模块概述

客户模块提供多渠道客户管理和 WebSocket 实时通信功能，支持 Web、微信、WhatsApp、Line 等 10+ 种渠道。

### 核心特性

✅ **多渠道支持** - Web、微信、WhatsApp、Line、Telegram、Facebook、Email、SMS、Phone、App  
✅ **快速接入** - 无需注册，自动创建客户  
✅ **实时通信** - WebSocket 双向通信  
✅ **灵活管理** - 标签、自定义字段、备注  
✅ **唯一性保证** - 邮箱、手机号、渠道 ID 唯一约束  
✅ **双重认证** - 同时支持客户 Token 和坐席 Token  

---

## 🚀 快速开始（5 分钟）

### 步骤 1: 创建数据库表

```bash
mysql -u root -p ai_agent < db/create_customers_table.sql
```

### 步骤 2: 启动应用

```bash
./mvnw spring-boot:run
```

### 步骤 3: 客户端获取 Token

```bash
curl -X POST http://127.0.0.1:8080/api/v1/public/customer-token \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试用户",
    "channel": "WEB",
    "channelId": "web_test_001"
  }'
```

### 步骤 4: 连接 WebSocket

```javascript
const token = "cust_xxx"; // 从步骤 3 获取
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到:', message.content);
};

ws.send(JSON.stringify({
  senderId: 'web_test_001',
  content: '你好，客服！'
}));
```

完成！🎉

---

## 📚 文档索引

| 文档 | 说明 |
|------|------|
| [CUSTOMER_INTEGRATION_GUIDE.md](./CUSTOMER_INTEGRATION_GUIDE.md) | 📖 **客户端完整接入指南**（推荐） |
| [CUSTOMER_API_SUMMARY.md](./CUSTOMER_API_SUMMARY.md) | 🔌 API 接口总结 |
| [WEBSOCKET_INTEGRATION_GUIDE.md](./WEBSOCKET_INTEGRATION_GUIDE.md) | 🌐 WebSocket 接入指南（坐席端） |

---

## 📡 API 速查

### 公开接口（无需认证）

```http
POST /api/v1/public/customer-token
```
快速获取客户 Token

### 客户管理接口（需要坐席认证）

```http
GET    /api/v1/customers              # 查询客户列表
GET    /api/v1/customers/{id}         # 获取客户详情
POST   /api/v1/customers              # 创建客户
PUT    /api/v1/customers/{id}         # 更新客户
DELETE /api/v1/customers/{id}         # 删除客户
POST   /api/v1/customers/{id}/token   # 为客户生成 Token
```

### WebSocket 端点

```
ws://127.0.0.1:8080/ws/chat?token={customer-or-agent-token}
```

---

## 🌍 支持的渠道

| 渠道 | Channel 枚举 | channelId 示例 |
|------|-------------|---------------|
| 网页 | `WEB` | `web_user_123` |
| 微信 | `WECHAT` | `oAbCd1234567890` (OpenID) |
| WhatsApp | `WHATSAPP` | `+8613800138000` |
| Line | `LINE` | `Uabcdef123456` |
| Telegram | `TELEGRAM` | `123456789` |
| Facebook | `FACEBOOK` | `1234567890123456` |
| 邮件 | `EMAIL` | `user@example.com` |
| 短信 | `SMS` | `+8613800138000` |
| 电话 | `PHONE` | `+8613800138000` |
| 应用 | `APP` | `app_user_uuid` |

---

## 💡 常见场景

### 场景 1: Web 聊天窗口

```javascript
// 1. 获取 Token
const response = await fetch('/api/v1/public/customer-token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: '访客',
    channel: 'WEB',
    channelId: 'web_' + generateUUID()
  })
});

const { data } = await response.json();

// 2. 连接 WebSocket
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${data.token}`);

// 3. 发送消息
ws.send(JSON.stringify({
  senderId: data.customerId,
  content: '我需要帮助'
}));
```

### 场景 2: 微信小程序

```javascript
// 获取客户 Token
wx.request({
  url: 'https://your-api.com/api/v1/public/customer-token',
  method: 'POST',
  data: {
    name: '微信用户',
    channel: 'WECHAT',
    channelId: wx.getStorageSync('openid')
  },
  success: (res) => {
    const token = res.data.data.token;
    // 连接 WebSocket
    wx.connectSocket({
      url: `wss://your-api.com/ws/chat?token=${token}`
    });
  }
});
```

### 场景 3: 坐席管理客户

```bash
# 查询所有微信渠道的客户
curl -X GET "http://127.0.0.1:8080/api/v1/customers?channel=WECHAT" \
  -H "Authorization: Bearer {agent-token}"

# 更新客户标签
curl -X PUT "http://127.0.0.1:8080/api/v1/customers/{customerId}" \
  -H "Authorization: Bearer {agent-token}" \
  -H "Content-Type: application/json" \
  -d '{"tags": ["VIP", "已购买"]}'
```

---

## 🔐 认证机制

### 双重认证系统

```
客户 Token (cust_xxxx)
  ↓
用于客户端连接 WebSocket
  ↓
自动创建，无需密码

坐席 Token (普通 UUID)
  ↓
用于管理客户信息
  ↓
需要邮箱密码登录
```

### Token 识别

```javascript
if (token.startsWith('cust_')) {
  // 客户身份
} else {
  // 坐席身份
}
```

---

## 🗄️ 数据模型

### Customer 实体

```java
@Entity
public class Customer {
    UUID id;
    String name;              // 客户姓名
    Channel primaryChannel;   // 主要渠道
    String email;             // 邮箱（唯一）
    String phone;             // 手机号（唯一）
    String wechatOpenId;      // 微信 OpenID（唯一）
    // ... 其他渠道 ID
    List<String> tags;        // 标签
    Map<String, Object> customFields;  // 自定义字段
    boolean active;           // 是否活跃
    Instant lastInteractionAt; // 最后交互时间
}
```

---

## ⚙️ 配置说明

### CORS 配置

在 `SecurityConfig.java` 中已配置：

```java
configuration.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "http://localhost:3001",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:3001"
));
```

生产环境请修改为实际域名。

### WebSocket 配置

支持两种连接方式：
- 原生 WebSocket: `ws://`
- SockJS: `http://` (自动降级)

---

## 🧪 测试示例

### Postman 测试集合

```json
{
  "info": {
    "name": "Customer Module Tests"
  },
  "item": [
    {
      "name": "Get Customer Token",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/v1/public/customer-token",
        "body": {
          "mode": "raw",
          "raw": "{\"name\":\"Test User\",\"channel\":\"WEB\",\"channelId\":\"test_001\"}"
        }
      }
    }
  ]
}
```

### 浏览器控制台测试

```javascript
// 1. 获取 Token
const getToken = async () => {
  const res = await fetch('http://127.0.0.1:8080/api/v1/public/customer-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: 'Browser Test',
      channel: 'WEB',
      channelId: 'web_' + Date.now()
    })
  });
  return (await res.json()).data.token;
};

// 2. 测试 WebSocket
const testWS = async () => {
  const token = await getToken();
  const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);
  
  ws.onopen = () => console.log('✅ Connected');
  ws.onmessage = (e) => console.log('📨', JSON.parse(e.data));
  
  setTimeout(() => {
    ws.send(JSON.stringify({
      senderId: 'test',
      content: 'Hello from browser!'
    }));
  }, 1000);
};

testWS();
```

---

## 🐛 故障排查

### 问题 1: WebSocket 连接失败

**检查清单**:
- [ ] 应用是否已启动
- [ ] Token 是否有效（以 `cust_` 开头）
- [ ] URL 格式正确：`ws://127.0.0.1:8080/ws/chat?token=xxx`
- [ ] 查看浏览器控制台和服务端日志

### 问题 2: CORS 错误

**解决方案**:
1. 检查 `SecurityConfig.java` 中的 `allowedOrigins`
2. 确保前端地址在允许列表中
3. 重启应用

### 问题 3: 客户重复创建

**原因**: `channelId` 不一致

**解决方案**: 确保同一客户使用相同的 `channelId`

---

## 📊 性能优化建议

### 生产环境优化

1. **Token 存储**: 将 `CustomerTokenService` 改为使用 Redis
   ```java
   @Service
   public class CustomerTokenService {
       @Autowired
       private RedisTemplate<String, UUID> redisTemplate;
       
       public String issueToken(Customer customer) {
           String token = "cust_" + UUID.randomUUID();
           redisTemplate.opsForValue().set(token, customer.getId(), 24, TimeUnit.HOURS);
           return token;
       }
   }
   ```

2. **数据库索引**: 已创建，参考 `create_customers_table.sql`

3. **连接池**: 配置 HikariCP
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
   ```

4. **WebSocket 集群**: 使用 Redis Pub/Sub 实现多实例消息广播

---

## 📝 待办事项

- [ ] 实现 Token 过期机制
- [ ] 添加客户合并功能
- [ ] 实现客户分组功能
- [ ] 添加客户搜索全文索引
- [ ] 实现 WebSocket 消息持久化
- [ ] 添加客户行为分析

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

MIT License

---

**项目维护**: AI KEF Team  
**最后更新**: 2024-01-15  
**版本**: v1.0.0
