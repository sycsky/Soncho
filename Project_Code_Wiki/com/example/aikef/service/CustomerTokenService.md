# CustomerTokenService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 客户认证 Token 管理服务。基于 Redis 实现轻量级的 Token 生成、验证和生命周期管理，用于客户端（Widget）用户的身份维持。

## 2. Method Deep Dive

### `issueToken`
- **Signature**: `public String issueToken(Customer customer)`
- **Description**: 为指定客户颁发一个新的 Token。
- **Logic**:
  1. 生成 UUID 作为 Token（前缀 `cust_`）。
  2. 存入 Redis，Key 为 `cust_token:{token}`，Value 为 `customerId`。
  3. 设置过期时间（默认 30 天）。

### `resolve`
- **Signature**: `public Optional<CustomerPrincipal> resolve(String token)`
- **Description**: 验证 Token 并解析出客户身份信息。
- **Logic**:
  1. 检查 Redis 中是否存在该 Token。
  2. 如果存在，根据 customerId 查询数据库获取最新客户信息。
  3. 返回 `CustomerPrincipal` 对象。

### `refreshToken`
- **Signature**: `public void refreshToken(String token)`
- **Description**: 延长 Token 的有效期。通常在每次验证成功后调用。

### `revoke`
- **Signature**: `public void revoke(String token)`
- **Description**: 立即注销 Token（删除 Redis Key）。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `CustomerRepository`: 客户信息查询。
  - `StringRedisTemplate`: Redis 操作。

## 4. Usage Guide
### 场景：访客自动登录
当客户在网站上打开聊天挂件时，前端从 LocalStorage 读取之前保存的 Token 发送给后端。后端调用 `resolve` 验证。
- 如果有效：自动识别为老客户，加载历史记录。
- 如果无效：创建新客户并调用 `issueToken` 返回新 Token。
