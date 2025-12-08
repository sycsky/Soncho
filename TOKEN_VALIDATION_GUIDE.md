# Token 验证接口使用指南

## 概述

为了让前端在建立 WebSocket 连接前能够验证 token 的有效性，我们提供了一个专门的 token 验证接口。

## API 接口

### 验证 Token

**端点**: `GET /api/v1/public/validate-token`

**参数**:
- `token` (查询参数，可选): Token 字符串
- `Authorization` (请求头，可选): Bearer Token

**说明**: 优先使用查询参数中的 token，如果没有则从 Authorization 头中获取。

### 请求示例

#### 方式 1: 使用查询参数

```bash
curl "http://localhost:8080/api/v1/public/validate-token?token=cust_xxx"
```

```javascript
const response = await fetch(
    'http://localhost:8080/api/v1/public/validate-token?token=cust_xxx'
);
const result = await response.json();
```

#### 方式 2: 使用 Authorization 头

```bash
curl -H "Authorization: Bearer cust_xxx" \
     "http://localhost:8080/api/v1/public/validate-token"
```

```javascript
const response = await fetch(
    'http://localhost:8080/api/v1/public/validate-token',
    {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    }
);
const result = await response.json();
```

### 响应格式

#### 客户 Token - 有效

```json
{
    "valid": true,
    "type": "customer",
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "channel": "WEB"
}
```

#### 客户 Token - 无效

```json
{
    "valid": false,
    "type": "customer",
    "error": "TOKEN_EXPIRED",
    "message": "客户 Token 无效或已过期"
}
```

#### 客服 Token - 有效

```json
{
    "valid": true,
    "type": "agent",
    "agentId": "550e8400-e29b-41d4-a716-446655440001",
    "username": "agent001"
}
```

#### 客服 Token - 无效

```json
{
    "valid": false,
    "type": "agent",
    "error": "TOKEN_EXPIRED",
    "message": "客服 Token 无效或已过期"
}
```

#### 缺少 Token

```json
{
    "valid": false,
    "error": "MISSING_TOKEN",
    "message": "缺少 token 参数"
}
```

## 前端集成

### 完整的 WebSocket 客户端实现

```javascript
class ChatWebSocket {
    constructor(baseUrl, token, apiBaseUrl) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.apiBaseUrl = apiBaseUrl;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 3;
        this.hasTriedRefreshToken = false;
    }

    /**
     * 推荐方式：连接前先验证 token
     */
    async connectWithValidation() {
        try {
            console.log('🔍 验证 token...');
            const validation = await this.validateToken();
            
            if (!validation.valid) {
                console.warn('⚠️ Token 无效:', validation.error, validation.message);
                
                // 根据错误类型处理
                if (validation.error === 'TOKEN_EXPIRED') {
                    console.log('🔄 尝试刷新 token...');
                    await this.refreshTokenAndConnect();
                } else {
                    throw new Error(validation.message);
                }
            } else {
                console.log('✅ Token 验证成功，建立连接...');
                this.connect();
            }
        } catch (error) {
            console.error('❌ 连接失败:', error);
            this.onConnectionStatusChange?.('error');
            throw error;
        }
    }

    /**
     * 验证 token 是否有效
     */
    async validateToken() {
        try {
            const response = await fetch(
                `${this.apiBaseUrl}/public/validate-token?token=${this.token}`
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('❌ Token 验证请求失败:', error);
            // 网络错误时返回验证失败
            return {
                valid: false,
                error: 'NETWORK_ERROR',
                message: '网络请求失败'
            };
        }
    }

    /**
     * 直接连接（不验证）
     */
    connect() {
        const wsUrl = `${this.baseUrl}?token=${this.token}`;
        
        console.log('🔌 正在连接 WebSocket...');
        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
            console.log('✅ WebSocket 连接成功');
            this.reconnectAttempts = 0;
            this.hasTriedRefreshToken = false;
            this.onConnectionStatusChange?.('connected');
        };

        this.ws.onerror = () => {
            console.error('❌ WebSocket 连接错误');
        };

        this.ws.onclose = (event) => {
            console.log('🔌 WebSocket 关闭:', {
                code: event.code,
                reason: event.reason,
                wasClean: event.wasClean
            });
            
            this.handleClose(event);
        };

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleMessage(data);
        };
    }

    handleClose(event) {
        this.onConnectionStatusChange?.('disconnected');
        
        if (event.code === 1000) {
            console.log('✅ 连接已正常关闭');
            return;
        }
        
        if (event.code === 1006 && !this.hasTriedRefreshToken) {
            // 首次遇到异常关闭，尝试刷新 token
            this.hasTriedRefreshToken = true;
            console.log('🔄 连接异常关闭，尝试刷新 token...');
            this.refreshTokenAndConnect().catch(() => {
                this.attemptReconnect();
            });
        } else {
            // 已经尝试过刷新或其他错误，普通重连
            this.attemptReconnect();
        }
    }

    async refreshTokenAndConnect() {
        if (this.isCustomer) {
            await this.refreshCustomerToken();
        } else {
            await this.refreshAgentToken();
        }
    }

    async refreshCustomerToken() {
        try {
            const response = await fetch(
                `${this.apiBaseUrl}/customers/${this.customerId}/token`,
                { method: 'POST' }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const data = await response.json();
            this.token = data.token;
            
            console.log('✅ Token 刷新成功，重新连接...');
            this.connect();
        } catch (error) {
            console.error('❌ 刷新客户 token 失败:', error);
            this.notifyUser('连接失败，请刷新页面重试');
            throw error;
        }
    }

    async refreshAgentToken() {
        console.warn('⚠️ 客服 Token 过期，需要重新登录');
        this.notifyUser('登录已过期，请重新登录');
        window.location.href = '/login';
    }

    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('❌ 达到最大重连次数');
            this.notifyUser('连接失败，请刷新页面重试');
            this.onConnectionStatusChange?.('error');
            return;
        }
        
        this.reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 10000);
        
        console.log(`🔄 ${delay}ms 后第 ${this.reconnectAttempts} 次重连...`);
        this.onConnectionStatusChange?.('reconnecting');
        
        setTimeout(() => this.connect(), delay);
    }

    handleMessage(data) {
        switch (data.type) {
            case 'message':
                this.onMessageReceived?.(data.message);
                break;
            case 'offline_message':
                this.onOfflineMessageReceived?.(data.message);
                break;
            case 'offline_messages_complete':
                this.onOfflineMessagesComplete?.(data.count);
                break;
            default:
                console.warn('未知消息类型:', data.type);
        }
    }

    sendMessage(sessionId, text) {
        if (this.ws?.readyState === WebSocket.OPEN) {
            const message = {
                type: 'message',
                sessionId: sessionId,
                text: text
            };
            this.ws.send(JSON.stringify(message));
        } else {
            console.error('❌ WebSocket 未连接');
            this.notifyUser('连接已断开，正在重新连接...');
            this.attemptReconnect();
        }
    }

    disconnect() {
        if (this.ws) {
            this.ws.close(1000, 'Client closed connection');
        }
    }

    notifyUser(message) {
        console.log('📢 通知用户:', message);
        // 实现 UI 通知逻辑
    }
}
```

## 使用示例

### 客户端完整流程

```javascript
// 1. 创建客户并获取 token
async function initCustomerChat() {
    try {
        // 创建客户
        const createResponse = await fetch('/api/v1/public/customer-token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: '张三',
                channel: 'WEB',
                email: 'zhangsan@example.com',
                phone: '13800138000'
            })
        });
        
        const customerData = await createResponse.json();
        console.log('客户创建成功:', customerData);
        
        // 2. 创建 WebSocket 客户端
        const wsClient = new ChatWebSocket(
            'ws://localhost:8080/ws/chat',
            customerData.token,
            '/api/v1'
        );
        
        wsClient.customerId = customerData.customerId;
        wsClient.isCustomer = true;
        
        // 3. 设置回调
        wsClient.onMessageReceived = (message) => {
            console.log('收到消息:', message);
            displayMessage(message);
        };
        
        wsClient.onOfflineMessageReceived = (message) => {
            console.log('离线消息:', message);
            displayOfflineMessage(message);
        };
        
        wsClient.onOfflineMessagesComplete = (count) => {
            console.log(`已加载 ${count} 条离线消息`);
        };
        
        wsClient.onConnectionStatusChange = (status) => {
            updateConnectionStatus(status);
        };
        
        // 4. 连接（推荐：先验证 token）
        await wsClient.connectWithValidation();
        
        return wsClient;
        
    } catch (error) {
        console.error('初始化聊天失败:', error);
        alert('连接失败，请刷新页面重试');
    }
}

// 启动聊天
const chatClient = await initCustomerChat();

// 发送消息
chatClient.sendMessage(sessionId, '你好，我需要帮助');
```

### 客服端完整流程

```javascript
// 1. 客服登录
async function initAgentChat() {
    try {
        // 登录获取 token
        const loginResponse = await fetch('/api/v1/public/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: 'agent001',
                password: 'password123'
            })
        });
        
        const loginData = await loginResponse.json();
        console.log('登录成功:', loginData);
        
        // 2. 创建 WebSocket 客户端
        const wsClient = new ChatWebSocket(
            'ws://localhost:8080/ws/chat',
            loginData.token,
            '/api/v1'
        );
        
        wsClient.isCustomer = false;
        
        // 3. 设置回调
        wsClient.onMessageReceived = (message) => {
            console.log('收到客户消息:', message);
            updateAgentWorkspace(message);
        };
        
        wsClient.onConnectionStatusChange = (status) => {
            updateConnectionStatus(status);
        };
        
        // 4. 连接（推荐：先验证 token）
        await wsClient.connectWithValidation();
        
        return wsClient;
        
    } catch (error) {
        console.error('初始化客服工作台失败:', error);
        alert('登录失败，请重试');
    }
}

// 启动客服工作台
const agentClient = await initAgentChat();
```

## 测试验证接口

### 使用 curl 测试

```bash
# 测试有效的客户 token
curl "http://localhost:8080/api/v1/public/validate-token?token=cust_xxx"

# 测试有效的客服 token
curl "http://localhost:8080/api/v1/public/validate-token?token=agent_xxx"

# 测试无效 token
curl "http://localhost:8080/api/v1/public/validate-token?token=invalid_token"

# 测试缺少 token
curl "http://localhost:8080/api/v1/public/validate-token"
```

### 使用 JavaScript 测试

```javascript
// 测试函数
async function testTokenValidation(token) {
    const response = await fetch(
        `http://localhost:8080/api/v1/public/validate-token?token=${token}`
    );
    const result = await response.json();
    console.log('验证结果:', result);
    return result;
}

// 测试有效 token
await testTokenValidation('cust_xxx');

// 测试无效 token
await testTokenValidation('invalid_token');
```

## 最佳实践

### 1. 连接前验证（推荐）

```javascript
// ✅ 好的做法
await wsClient.connectWithValidation();
```

这种方式可以：
- 在连接前就知道 token 是否有效
- 避免无效 token 导致的握手失败
- 提供更好的用户体验（明确的错误提示）

### 2. 定期验证（可选）

```javascript
// 定期检查 token 是否仍然有效
setInterval(async () => {
    const validation = await wsClient.validateToken();
    if (!validation.valid) {
        console.warn('Token 已失效，刷新中...');
        await wsClient.refreshTokenAndConnect();
    }
}, 5 * 60 * 1000); // 每 5 分钟检查一次
```

### 3. 错误处理

```javascript
try {
    await wsClient.connectWithValidation();
} catch (error) {
    if (error.message.includes('TOKEN_EXPIRED')) {
        // Token 过期，引导用户刷新
        showRefreshPrompt();
    } else if (error.message.includes('NETWORK_ERROR')) {
        // 网络错误，稍后重试
        scheduleRetry();
    } else {
        // 其他错误
        showErrorMessage(error.message);
    }
}
```

## 常见问题

### Q: 为什么需要 token 验证接口？

A: 因为浏览器的 WebSocket API 无法直接读取握手失败时的 HTTP 响应头。通过在连接前先调用验证接口，可以：
- 明确知道 token 是否有效
- 获取详细的错误信息
- 提前刷新过期的 token
- 提供更好的用户体验

### Q: 验证接口会增加网络请求吗？

A: 会增加一次 HTTP 请求，但这是值得的：
- 请求非常轻量（只验证 token）
- 可以避免 WebSocket 握手失败
- 可以提前发现并解决 token 问题
- 总体上减少了重连次数

### Q: 是否必须使用验证接口？

A: 不是必须的，但强烈推荐：
- **不使用验证接口**：直接连接，握手失败时只能得到 `code=1006`，需要猜测是否是 token 问题
- **使用验证接口**：连接前就知道 token 状态，可以提前处理

### Q: 验证接口的响应时间如何？

A: 非常快，通常在 10-50ms 内完成（本地验证，不需要数据库查询）

## 安全建议

1. **不要在日志中记录完整的 token**
2. **使用 HTTPS/WSS**（生产环境）
3. **实施 token 过期策略**
4. **限制验证接口的调用频率**（防止暴力破解）
5. **不要在 URL 中长期暴露 token**

## 总结

通过使用 token 验证接口，可以：
- ✅ 在连接前明确知道 token 是否有效
- ✅ 获取详细的错误信息
- ✅ 提前刷新过期的 token
- ✅ 提供更好的用户体验
- ✅ 减少无效的 WebSocket 连接尝试

这是处理 WebSocket token 验证的最佳实践！
