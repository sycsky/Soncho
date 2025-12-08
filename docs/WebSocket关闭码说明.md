# WebSocket 关闭码说明

## 关闭码的来源

### 1006 关闭码的特殊性

**重要：`1006` 是浏览器自动设置的，不能由应用程序代码设置！**

当 WebSocket 握手失败或连接异常终止时，浏览器会自动使用 `1006` 作为关闭码。

### 实际流程

```
Token 验证失败
    ↓
TokenHandshakeInterceptor.beforeHandshake() 返回 false
    ↓
Spring 框架拒绝握手
    ↓
返回 HTTP 401 响应（包含错误响应头）
    ↓
连接从未建立（WebSocket 层面）
    ↓
浏览器检测到握手失败
    ↓
浏览器自动设置 code=1006  ← 这里！
    ↓
触发前端 onclose 事件
```

## Java 代码中如何设置关闭码

### 场景 1：握手阶段拒绝连接

**当前实现方式：**

```java
@Override
public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Map<String, Object> attributes) {
    if (token 无效) {
        // 设置 HTTP 响应
        response.setStatusCode(HttpStatus.UNAUTHORIZED);  // 401
        response.getHeaders().add("X-WebSocket-Error-Code", "TOKEN_EXPIRED");
        
        // 拒绝握手
        return false;  // ← 这会导致浏览器设置 code=1006
    }
    
    return true;
}
```

**结果：**
- 浏览器收到 HTTP 401
- 前端 `onclose` 事件中 `event.code = 1006`（浏览器自动设置）

### 场景 2：连接建立后主动关闭

如果你想在连接建立后主动关闭并设置自定义关闭码：

```java
// 在 ChatWebSocketHandler 中
@Override
public void handleTextMessage(WebSocketSession session, TextMessage message) {
    // 验证消息
    if (需要关闭连接) {
        try {
            // 设置自定义关闭码
            session.close(CloseStatus.POLICY_VIOLATION);  // 1008 - 策略违规
            // 或者
            session.close(new CloseStatus(4001, "Token expired"));  // 自定义码
        } catch (IOException e) {
            log.error("关闭连接失败", e);
        }
    }
}
```

**可用的 Spring CloseStatus 常量：**

```java
CloseStatus.NORMAL                  // 1000 - 正常关闭
CloseStatus.GOING_AWAY             // 1001 - 端点离开
CloseStatus.PROTOCOL_ERROR         // 1002 - 协议错误
CloseStatus.NOT_ACCEPTABLE         // 1003 - 不可接受的数据类型
CloseStatus.NO_STATUS_CODE         // 1005 - 未收到状态码
CloseStatus.NO_CLOSE_FRAME         // 1006 - 异常关闭（不能使用！）
CloseStatus.BAD_DATA               // 1007 - 无效数据
CloseStatus.POLICY_VIOLATION       // 1008 - 策略违规
CloseStatus.TOO_BIG_TO_PROCESS     // 1009 - 消息太大
CloseStatus.REQUIRED_EXTENSION     // 1010 - 需要扩展
CloseStatus.SERVER_ERROR           // 1011 - 服务器错误
CloseStatus.SERVICE_RESTARTED      // 1012 - 服务重启
CloseStatus.TRY_AGAIN_LATER        // 1013 - 稍后重试

// 自定义关闭码（4000-4999）
new CloseStatus(4001, "Custom reason")
```

### 场景 3：运行时 Token 验证失败

如果想在运行时检测到 Token 过期后优雅关闭：

```java
@Override
public void handleTextMessage(WebSocketSession session, TextMessage message) {
    // 获取当前用户
    AgentPrincipal agent = (AgentPrincipal) session.getAttributes().get("AGENT_PRINCIPAL");
    
    // 验证 Token 是否仍然有效
    if (agent != null && !tokenService.isValid(agent.getToken())) {
        try {
            // 发送错误消息
            Map<String, Object> errorMsg = Map.of(
                "type", "error",
                "code", "TOKEN_EXPIRED",
                "message", "Token 已过期，请重新登录"
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
            
            // 延迟关闭，让消息先发送
            Thread.sleep(100);
            
            // 关闭连接
            session.close(new CloseStatus(4001, "Token expired"));
        } catch (Exception e) {
            log.error("关闭连接失败", e);
        }
    }
    
    // 处理正常消息...
}
```

**前端接收到的关闭码：**

```javascript
ws.onclose = (event) => {
    console.log('关闭码:', event.code);     // 4001
    console.log('关闭原因:', event.reason);  // "Token expired"
    
    if (event.code === 4001) {
        // Token 过期，刷新后重连
        await refreshToken();
        reconnect();
    }
};
```

## 推荐的 Token 过期处理方案

### 方案 A：握手阶段拒绝（当前实现）

**优点：**
- 简单直接
- 不消耗服务器资源（连接未建立）

**缺点：**
- 前端只能收到 `code=1006`，无法明确知道是 token 问题
- 需要通过其他方式（如连接前验证）判断

### 方案 B：连接建立后发送错误消息再关闭

修改 `TokenHandshakeInterceptor`，允许所有连接，然后在 `afterConnectionEstablished` 中验证：

```java
// 修改后的实现（不推荐，仅供参考）
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    AgentPrincipal agent = (AgentPrincipal) session.getAttributes().get("AGENT_PRINCIPAL");
    CustomerPrincipal customer = (CustomerPrincipal) session.getAttributes().get("CUSTOMER_PRINCIPAL");
    
    if (agent == null && customer == null) {
        // Token 验证失败
        try {
            // 发送错误消息
            Map<String, Object> error = Map.of(
                "type", "error",
                "code", "TOKEN_EXPIRED",
                "message", "Token 无效或已过期"
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            
            // 关闭连接
            session.close(new CloseStatus(4001, "Token expired"));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
        return;
    }
    
    // 正常处理...
}
```

**优点：**
- 前端可以收到明确的错误消息和自定义关闭码
- 用户体验更好

**缺点：**
- 需要建立连接（消耗资源）
- 稍微复杂一些

### 方案 C：混合方案（推荐）

1. **握手阶段**：拦截明显无效的 token（如格式错误、缺失）
2. **连接阶段**：定期检查 token 是否过期，过期时发送消息后关闭

```java
// TokenHandshakeInterceptor - 基本验证
public boolean beforeHandshake(...) {
    if (token == null || token.isBlank()) {
        return false;  // 拒绝握手
    }
    
    // 基本格式验证通过，允许建立连接
    // 详细验证延后到 afterConnectionEstablished
    return true;
}

// ChatWebSocketHandler - 详细验证
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    Principal principal = getPrincipal(session);
    
    if (principal == null || isTokenExpired(principal)) {
        sendErrorAndClose(session, "TOKEN_EXPIRED", 4001);
        return;
    }
    
    // 正常处理...
}

// 消息处理时也验证
@Override
public void handleTextMessage(WebSocketSession session, TextMessage message) {
    if (isTokenExpired(session)) {
        sendErrorAndClose(session, "TOKEN_EXPIRED", 4001);
        return;
    }
    
    // 处理消息...
}
```

## 关闭码对照表

| 关闭码 | 名称 | 设置者 | 用途 |
|--------|------|--------|------|
| 1000 | Normal Closure | Java 代码 | 正常关闭 |
| 1001 | Going Away | Java 代码 | 服务器关闭/页面跳转 |
| 1002 | Protocol Error | 框架 | 协议错误 |
| 1003 | Unsupported Data | Java 代码 | 不支持的数据类型 |
| 1006 | **Abnormal Closure** | **浏览器** | **异常关闭（不能设置）** |
| 1008 | Policy Violation | Java 代码 | 策略违规（适合认证失败） |
| 1011 | Server Error | Java 代码 | 服务器内部错误 |
| 4000-4999 | 自定义 | Java 代码 | 应用自定义错误 |

## 建议的自定义关闭码

```java
public class WebSocketCloseCode {
    public static final int TOKEN_EXPIRED = 4001;       // Token 过期
    public static final int PERMISSION_DENIED = 4002;   // 权限不足
    public static final int RATE_LIMIT = 4003;          // 请求频率过高
    public static final int INVALID_MESSAGE = 4004;     // 无效消息格式
    public static final int SESSION_NOT_FOUND = 4005;   // 会话不存在
}
```

## 总结

1. **`1006` 是浏览器自动设置的**，Java 代码无法设置，也不应该尝试设置
2. **握手失败** → 浏览器设置 `code=1006`
3. **连接建立后关闭** → Java 代码可以设置自定义关闭码（建议使用 4000-4999）
4. **推荐使用 `CloseStatus.POLICY_VIOLATION` (1008) 或自定义码 (4001) 处理认证失败**
5. **最佳实践**：连接前验证 token + 运行时定期检查 + 发送错误消息后关闭
