# WebSocket Token 过期错误处理指南

## 概述

当 WebSocket 连接时 token 无效或过期，系统会通过 HTTP 响应头返回明确的错误信息，前端可以根据这些信息进行相应处理。

## 错误码说明

| 错误码 | 含义 | 处理建议 |
|--------|------|----------|
| `MISSING_TOKEN` | 缺少 token 参数 | 检查连接 URL 是否包含 token 参数 |
| `TOKEN_EXPIRED` | Token 无效或已过期 | 重新获取 token 后再次连接 |
| `INVALID_REQUEST` | 无效的请求类型 | 检查请求格式 |

## 前端处理示例

### 方案说明

**重要提示：** 由于浏览器 WebSocket API 的限制，握手失败时无法直接读取 HTTP 响应头。虽然服务端返回了 `401` 状态码和错误响应头，但前端只能通过 `onclose` 事件的 `code=1006` 来推测可能是认证问题。

**推荐做法：** 在建立 WebSocket 连接前，先通过 REST API 验证 token 是否有效。

### JavaScript 原生 WebSocket

```javascript
class ChatWebSocket {
    constructor(baseUrl, token, apiBaseUrl) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.apiBaseUrl = apiBaseUrl; // REST API 基础 URL
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 3;
        this.isCustomer = false;
        this.customerId = null;
        this.channel = null;
    }

    /**
     * 连接前先验证 token（推荐）
     */
    async connectWithValidation() {
        try {
            // 先验证 token 是否有效
            const isValid = await this.validateToken();
            
            if (!isValid) {
                console.warn('⚠️ Token 无效，正在刷新...');
                await this.refreshTokenAndConnect();
            } else {
                this.connect();
            }
        } catch (error) {
            console.error('❌ 连接失败:', error);
            this.notifyUser('连接失败，请稍后重试');
        }
    }

    /**
     * 验证 token 是否有效（可选的额外验证）
     */
    async validateToken() {
        try {
            // 调用专门的 token 验证接口
            const response = await fetch(
                `${this.apiBaseUrl}/public/validate-token?token=${this.token}`
            );
            
            if (!response.ok) {
                return false;
            }
            
            const result = await response.json();
            
            if (result.valid) {
                console.log('✅ Token 验证成功:', result);
                return true;
            } else {
                console.warn('⚠️ Token 验证失败:', result.error, result.message);
                return false;
            }
        } catch (error) {
            console.error('❌ Token 验证请求失败:', error);
            return false;
        }
    }

    /**
     * 直接连接（不验证）
     */
    connect() {
        const wsUrl = `${this.baseUrl}?token=${this.token}`;
        
        console.log('🔌 正在连接 WebSocket...');
        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = (event) => {
            console.log('✅ WebSocket 连接成功');
            this.reconnectAttempts = 0;
            this.onConnectionStatusChange?.('connected');
        };

        this.ws.onerror = (error) => {
            console.error('❌ WebSocket 连接错误');
            // 注意：error 对象不包含详细信息
            // 实际的错误信息会在 onclose 事件中体现
        };

        this.ws.onclose = (event) => {
            console.log('🔌 WebSocket 连接关闭:', {
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

    /**
     * 处理连接关闭
     */
    handleClose(event) {
        this.onConnectionStatusChange?.('disconnected');
        
        // WebSocket 关闭码说明：
        // 1000 = 正常关闭
        // 1006 = 异常关闭（通常是握手失败、网络问题）
        // 其他 = 各种错误情况
        
        if (event.code === 1000) {
            // 正常关闭，不需要重连
            console.log('✅ 连接已正常关闭');
            return;
        }
        
        if (event.code === 1006) {
            // 异常关闭，可能的原因：
            // 1. Token 验证失败（握手阶段被拒绝）
            // 2. 网络问题
            // 3. 服务器异常
            
            console.warn('⚠️ 连接异常关闭 (code=1006)，可能是 token 问题');
            
            // 策略：先尝试刷新 token，如果还是失败再重连
            this.handlePossibleTokenExpired();
        } else {
            // 其他非正常关闭，尝试重连
            console.warn(`⚠️ 连接非正常关闭 (code=${event.code})`);
            this.attemptReconnect();
        }
    }

    /**
     * 处理可能的 token 过期
     */
    async handlePossibleTokenExpired() {
        // 首次遇到 1006 时，先尝试刷新 token
        if (!this.hasTriedRefreshToken) {
            this.hasTriedRefreshToken = true;
            console.log('🔄 尝试刷新 token...');
            
            try {
                await this.refreshTokenAndConnect();
            } catch (error) {
                console.error('❌ 刷新 token 失败，尝试普通重连');
                this.attemptReconnect();
            }
        } else {
            // 已经尝试过刷新 token，这次直接重连
            this.attemptReconnect();
        }
    }

    /**
     * 刷新 token 并重新连接
     */
    async refreshTokenAndConnect() {
        if (this.isCustomer) {
            await this.refreshCustomerToken();
        } else {
            await this.refreshAgentToken();
        }
    }



    /**
     * 刷新客户 token
     */
    async refreshCustomerToken() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/customers/token`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    customerId: this.customerId,
                    channel: this.channel
                })
            });
            
            if (!response.ok) {
                throw new Error(`刷新失败: ${response.status}`);
            }
            
            const data = await response.json();
            this.token = data.token;
            this.hasTriedRefreshToken = false; // 重置标记
            
            console.log('✅ Token 刷新成功，重新连接...');
            this.connect();
        } catch (error) {
            console.error('❌ 刷新客户 token 失败:', error);
            this.notifyUser('连接失败，请刷新页面重试');
            throw error;
        }
    }

    /**
     * 刷新客服 token（需要重新登录）
     */
    async refreshAgentToken() {
        console.warn('⚠️ 客服 Token 过期，需要重新登录');
        this.notifyUser('登录已过期，请重新登录');
        
        // 可选：如果保存了登录凭证，可以尝试静默重登
        // const savedCredentials = this.getSavedCredentials();
        // if (savedCredentials) {
        //     await this.relogin(savedCredentials);
        // } else {
        //     window.location.href = '/login';
        // }
        
        window.location.href = '/login';
    }

    /**
     * 尝试重连（使用指数退避）
     */
    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('❌ 达到最大重连次数，停止重连');
            this.notifyUser('连接失败，请刷新页面重试');
            this.onConnectionStatusChange?.('error');
            return;
        }
        
        this.reconnectAttempts++;
        
        // 指数退避：1s, 2s, 4s, 8s, 最大 10s
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 10000);
        
        console.log(`🔄 ${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连...`);
        this.onConnectionStatusChange?.('reconnecting');
        
        setTimeout(() => {
            this.connect();
        }, delay);
    }

    handleMessage(data) {
        switch (data.type) {
            case 'message':
                this.onMessageReceived(data.message);
                break;
            case 'offline_message':
                this.onOfflineMessageReceived(data.message);
                break;
            case 'offline_messages_complete':
                this.onOfflineMessagesComplete(data.count);
                break;
            default:
                console.warn('未知消息类型:', data.type);
        }
    }

    sendMessage(sessionId, text) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            const message = {
                type: 'message',
                sessionId: sessionId,
                text: text
            };
            this.ws.send(JSON.stringify(message));
        } else {
            console.error('❌ WebSocket 未连接，无法发送消息');
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
        // 实现用户通知逻辑（Toast、Alert 等）
        console.log('📢 通知用户:', message);
    }
}
```

### 使用示例

#### 方法 1：连接前验证 token（推荐）

```javascript
// 创建客户并获取 token
const response = await fetch('/api/v1/customers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        name: '张三',
        channel: 'WEB',
        metadata: { source: 'homepage' }
    })
});

const customerData = await response.json();

// 创建 WebSocket 客户端
const wsClient = new ChatWebSocket(
    'ws://localhost:8080/ws/chat',  // WebSocket URL
    customerData.token,              // Token
    '/api/v1'                        // REST API 基础 URL
);

wsClient.customerId = customerData.id;
wsClient.channel = 'WEB';
wsClient.isCustomer = true;

// 设置回调
wsClient.onMessageReceived = (message) => {
    console.log('收到消息:', message);
};

wsClient.onConnectionStatusChange = (status) => {
    console.log('连接状态:', status);
    // 更新 UI 显示连接状态
};

// **推荐方式：连接前先验证 token**
await wsClient.connectWithValidation();
```

#### 方法 2：直接连接（不验证）

```javascript
// 如果你确定 token 是刚获取的，可以直接连接
wsClient.connect();

// 连接会自动处理可能的 token 过期问题
```

## 核心要点总结

### 为什么前端无法直接获取 HTTP 401 错误？

1. **WebSocket 握手是 HTTP**：第一步是 HTTP 握手请求
2. **服务端确实返回了 401 + 错误响应头**：`X-WebSocket-Error-Code` 和 `X-WebSocket-Error-Message`
3. **但浏览器 API 的限制**：WebSocket API 设计上不允许 JavaScript 读取握手的 HTTP 响应头
4. **只能得到关闭码**：握手失败会触发 `onclose`，`event.code` 通常是 `1006`

### 真实的错误处理流程

```
用户发起连接
    ↓
WebSocket 握手 (HTTP)
    ↓
服务端验证 Token
    ↓
┌─────────────────────────┐
│  Token 有效？           │
└─────────────────────────┘
    │              │
   YES            NO
    │              │
    │              ↓
    │         返回 HTTP 401
    │         X-WebSocket-Error-Code: TOKEN_EXPIRED
    │         X-WebSocket-Error-Message: Token 无效或已过期
    │              │
    │              ↓
    │         浏览器看到 401，握手失败
    │              │
    │              ↓
    │         触发 ws.onerror (无详细信息)
    │              │
    │              ↓
    │         触发 ws.onclose (code=1006)
    │              │
    │              ↓
    │         前端检测到 code=1006
    │              │
    │              ↓
    │         推测可能是 token 问题
    │              │
    │              ↓
    │         刷新 token
    │              │
    │              ↓
    │         重新连接
    │              │
    ↓              ↓
升级为 WebSocket 协议
    ↓
连接建立成功
    ↓
推送离线消息
    ↓
正常通信
```

### 最佳实践

**推荐方案：连接前验证 token**

```javascript
// ✅ 好的做法
async function connectWebSocket() {
    // 1. 先通过 REST API 验证 token
    const isValid = await validateToken(token);
    
    if (!isValid) {
        // 2. Token 无效，先刷新
        token = await refreshToken();
    }
    
    // 3. 使用有效的 token 建立连接
    ws.connect(token);
}
```

**退而求其次：处理 code=1006**

```javascript
// ⚠️ 次优做法（无法提前验证）
ws.onclose = (event) => {
    if (event.code === 1006) {
        // 可能是 token 问题，尝试刷新
        refreshTokenAndReconnect();
    }
};
```

### 2. Token 刷新策略

**客户端：**
- Token 相对简单，可以直接调用 `/api/v1/customers/token` 重新获取
- 建议实现自动重连机制

**客服端：**
- Token 过期意味着登录会话失效
- 应该引导用户重新登录，而不是自动刷新
- 可以存储登录凭证实现静默重登（注意安全性）

### 3. 重连策略

建议使用指数退避算法：
- 第1次重连：延迟 2 秒
- 第2次重连：延迟 4 秒
- 第3次重连：延迟 8 秒
- 最大延迟不超过 10 秒
- 最多重连 3-5 次

### 4. 用户体验优化

```javascript
class ChatUI {
    showConnectionStatus(status) {
        const statusBar = document.getElementById('connection-status');
        
        switch (status) {
            case 'connecting':
                statusBar.className = 'status-connecting';
                statusBar.textContent = '正在连接...';
                break;
            case 'connected':
                statusBar.className = 'status-connected';
                statusBar.textContent = '已连接';
                setTimeout(() => statusBar.style.display = 'none', 2000);
                break;
            case 'disconnected':
                statusBar.className = 'status-disconnected';
                statusBar.textContent = '连接已断开';
                break;
            case 'reconnecting':
                statusBar.className = 'status-reconnecting';
                statusBar.textContent = '正在重新连接...';
                break;
            case 'error':
                statusBar.className = 'status-error';
                statusBar.textContent = '连接失败，请重试';
                break;
        }
        
        statusBar.style.display = 'block';
    }

    disableSendButton() {
        const sendBtn = document.getElementById('send-button');
        sendBtn.disabled = true;
        sendBtn.textContent = '连接中...';
    }

    enableSendButton() {
        const sendBtn = document.getElementById('send-button');
        sendBtn.disabled = false;
        sendBtn.textContent = '发送';
    }
}
```

## 服务端响应头说明

当 WebSocket 握手失败时，服务端会返回以下响应头：

```
HTTP/1.1 401 Unauthorized
X-WebSocket-Error-Code: TOKEN_EXPIRED
X-WebSocket-Error-Message: Token 无效或已过期，请重新获取
```

虽然浏览器 WebSocket API 无法直接读取这些头，但它们会出现在网络请求日志中，便于调试。

## 调试技巧

### Chrome DevTools

1. 打开 **Network** 标签
2. 筛选 **WS**（WebSocket）
3. 点击 WebSocket 连接
4. 查看 **Headers** 标签页：
   - 如果握手失败，状态码会显示 `401`
   - **Response Headers** 中会包含 `X-WebSocket-Error-Code` 和 `X-WebSocket-Error-Message`

### 日志增强

```javascript
class DebugWebSocket extends ChatWebSocket {
    connect() {
        console.group('🔌 WebSocket 连接');
        console.log('URL:', `${this.baseUrl}?token=${this.maskToken(this.token)}`);
        console.log('时间:', new Date().toISOString());
        console.groupEnd();
        
        super.connect();
        
        // 记录所有事件
        this.ws.addEventListener('open', (e) => {
            console.log('✅ open 事件:', e);
        });
        
        this.ws.addEventListener('error', (e) => {
            console.error('❌ error 事件:', e);
        });
        
        this.ws.addEventListener('close', (e) => {
            console.group('🔌 close 事件');
            console.log('Code:', e.code);
            console.log('Reason:', e.reason);
            console.log('WasClean:', e.wasClean);
            console.groupEnd();
        });
    }

    maskToken(token) {
        if (!token || token.length < 10) return '***';
        return token.substring(0, 8) + '...' + token.substring(token.length - 4);
    }
}
```

## 完整错误处理流程

```
用户发起连接
    ↓
WebSocket 握手
    ↓
Token 验证
    ↓
┌─────────────────┐
│  验证成功？     │
└─────────────────┘
    │         │
   Yes       No
    │         │
    │         ↓
    │    返回 401 + 错误头
    │         ↓
    │    触发 onerror
    │         ↓
    │    触发 onclose (code=1006)
    │         ↓
    │    前端检测 code=1006
    │         ↓
    │    判断用户类型
    │         │
    │    ┌────┴────┐
    │    │         │
    │  客户      客服
    │    │         │
    │    ↓         ↓
    │  刷新token  重新登录
    │    │
    │    ↓
    │  重新连接
    │    │
    ↓    ↓
连接建立成功
    ↓
推送离线消息
    ↓
正常通信
```

## 相关 API

### Token 管理

- **创建客户并获取 token**: `POST /api/v1/public/customer-token`
  ```json
  {
    "name": "张三",
    "channel": "WEB",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "channelUserId": "web_user_123"
  }
  ```
  
  响应：
  ```json
  {
    "customerId": "uuid",
    "token": "cust_xxx",
    "sessionId": "uuid",
    "expiresAt": "2024-01-01T12:00:00Z"
  }
  ```

- **验证 token 是否有效**: `GET /api/v1/public/validate-token?token=xxx`
  
  响应（有效）：
  ```json
  {
    "valid": true,
    "type": "customer",  // 或 "agent"
    "customerId": "uuid",
    "name": "张三",
    "channel": "WEB"
  }
  ```
  
  响应（无效）：
  ```json
  {
    "valid": false,
    "type": "customer",
    "error": "TOKEN_EXPIRED",
    "message": "客户 Token 无效或已过期"
  }
  ```

- **客户刷新 token**: `POST /api/v1/customers/{customerId}/token`

- **客服登录获取 token**: `POST /api/v1/public/login`

### WebSocket 连接

- **WebSocket 连接**: `ws://your-domain/ws/chat?token=xxx`

## 安全建议

1. **不要在 URL 中长期暴露 token**：建议在连接成功后，从 URL 中移除 token 参数
2. **实施 token 过期策略**：建议 token 有效期为 24 小时（客户）或 8 小时（客服）
3. **限制重连次数**：防止无效 token 反复重连
4. **使用 HTTPS/WSS**：生产环境必须使用加密连接

## 示例场景

### 场景 1：客户长时间未活动后重新使用

1. 客户打开页面（token 已过期）
2. WebSocket 连接失败（握手返回 401）
3. 触发 `onclose` 事件（code=1006）
4. 自动调用 `/api/v1/customers/token` 获取新 token
5. 使用新 token 重新连接
6. 连接成功，推送离线消息

### 场景 2：客服 token 过期

1. 客服登录工作 8 小时后，token 过期
2. WebSocket 连接断开
3. 前端检测到 token 过期
4. 提示"登录已过期，请重新登录"
5. 跳转到登录页面

### 场景 3：网络波动导致断线

1. 网络暂时中断
2. 触发 `onclose` 事件（code 可能是 1006 或其他）
3. 如果不是 code=1006，直接尝试重连（不刷新 token）
4. 使用原 token 重新连接
5. 连接成功继续使用
