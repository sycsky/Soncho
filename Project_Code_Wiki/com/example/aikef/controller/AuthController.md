# AuthController

## 1. 类档案 (Class Profile)
- **功能定义**：认证控制器，处理坐席登录、Token 颁发及获取当前用户信息。
- **注解与配置**：
  - `@RestController`: REST API 控制器。
  - `@RequestMapping("/api/v1/auth")`: 基础路径。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `login` | In: `LoginRequest`<br>Out: `LoginResponse` | 1. 调用 `authenticationManager.authenticate` 进行认证。<br>2. 生成 JWT Token。<br>3. 获取坐席详细信息并返回。 | 依赖 Spring Security 的认证机制。 |
| `me` | Out: `AgentDto` | 获取当前已认证的坐席信息。 | 需要 Token 才能调用。 |

## 3. 依赖全景 (Dependency Graph)
- **`AuthenticationManager`**: Spring Security 认证管理器。
- **`TokenService`**: Token 生成服务。
- **`AgentService`**: 获取坐席信息。

## 4. 调用指南 (Usage Guide)
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "agent@example.com",
  "password": "password"
}
```
