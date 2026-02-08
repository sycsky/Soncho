# AgentService

## 1. 类档案 (Class Profile)
- **功能定义**：坐席（客服）管理服务，负责坐席的增删改查、认证、权限管理以及当前登录用户上下文获取。
- **注解与配置**：
  - `@Service`: 标记为 Spring 服务。
  - `@Transactional(readOnly = true)`: 默认只读事务。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `createAgent` | In: `CreateAgentRequest`<br>Out: `AgentDto` | 1. 校验邮箱唯一性。<br>2. 验证 Role 是否存在。<br>3. 加密密码 (`PasswordEncoder`)。<br>4. 设置租户 ID (SAAS 场景)。<br>5. 保存并返回 DTO。 | 包含租户上下文处理逻辑。 |
| `updateAgent` | In: `agentId`, `UpdateAgentRequest`<br>Out: `AgentDto` | 1. 查找 Agent。<br>2. 校验邮箱变更唯一性。<br>3. 更新非空字段（动态更新）。<br>4. 保存。 | 使用 Optional 判空和条件更新。 |
| `listAgents` | In: `name`, `role`, `pageable`<br>Out: `Page<AgentDto>` | 使用 JPA Specification 动态构建查询条件（模糊匹配 name，精确匹配 role）。 | |
| `currentAgent` | Out: `AgentDto` | 从 Spring Security 上下文 (`CurrentAgentProvider`) 获取当前登录用户 ID，再查询数据库。 | 抛出 `EntityNotFoundException` 如果未登录。 |

## 3. 依赖全景 (Dependency Graph)
- **`AgentRepository`**: 数据访问。
- **`RoleRepository`**: 角色关联。
- **`PasswordEncoder`**: 密码加密（BCrypt 等）。
- **`CurrentAgentProvider`**: 安全上下文封装。

## 4. 调用指南 (Usage Guide)
```java
@Autowired
private AgentService agentService;

// 创建新客服
CreateAgentRequest req = new CreateAgentRequest("John", "john@example.com", "pass123", roleId);
agentService.createAgent(req);
```
