# CustomerManagementTools

## 1. 类档案 (Class Profile)
- **功能定义**：客户管理工具集，提供给 AI 查询和更新当前会话客户信息的能力。
- **注解与配置**：
  - `@Component`: 注册为 Spring 组件。
  - `@Tool`: 暴露给 AI 使用。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `getSelfCustomerInfo` | In: `sessionId`<br>Out: `String` (JSON) | 1. 根据 sessionId 查找会话。<br>2. 获取关联的 Customer。<br>3. 转换为 DTO 并序列化返回。 | |
| `updateSelfCustomerInfo` | In: `sessionId`, `name`, `email`...<br>Out: `String` (Success/Error) | 1. 查找会话和客户。<br>2. 构建 `UpdateCustomerRequest`。<br>3. 调用 Service 更新客户信息。 | 只允许更新部分基本信息（如姓名、联系方式），不允许修改渠道绑定。 |
| `setCustomerRole` | In: `sessionId`, `roleCode`<br>Out: `String` | 1. 查找客户。<br>2. 调用 `SpecialCustomerService` 分配特殊角色。 | 用于业务场景（如认证为"供应商"）。 |

## 3. 依赖全景 (Dependency Graph)
- **`ChatSessionService`**: 查找会话上下文。
- **`CustomerService`**: 更新客户信息。
- **`SpecialCustomerService`**: 角色分配。

## 4. 调用指南 (Usage Guide)
**AI 调用示例**：
`updateSelfCustomerInfo(sessionId="...", email="new@email.com")`
