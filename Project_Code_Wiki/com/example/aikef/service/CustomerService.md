# CustomerService

## 1. 类档案 (Class Profile)
- **功能定义**：客户档案管理服务。管理客户的基本信息、渠道身份（OpenID 等）、自定义字段及最后交互时间。
- **注解与配置**：
  - `@Service`: 标记为 Spring 服务。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `createCustomer` | In: `CreateCustomerRequest`<br>Out: `CustomerDto` | 1. 校验邮箱和手机号唯一性。<br>2. 填充多渠道 ID (WeChat, WhatsApp, etc.)。<br>3. 保存客户档案。 | 聚合了多个社交渠道的身份 ID。 |
| `listCustomers` | In: `name`, `channel`, `tag`...<br>Out: `Page<CustomerDto>` | 动态查询，支持按 JSON 数组类型的 `tags` 字段进行模糊匹配 (`LIKE %"tag"%`)。 | 特殊的 JSON 字段查询处理。 |
| `updateLastInteraction` | In: `customerId` | 更新 `lastInteractionAt` 字段。 | 用于活跃度分析和清理策略。 |

## 3. 依赖全景 (Dependency Graph)
- **`CustomerRepository`**: 数据访问。
- **`CustomerTokenService`**: 处理客户认证 Token（虽注入但本类未深度使用，可能在其他方法中用到）。

## 4. 调用指南 (Usage Guide)
```java
// 根据 Webhook 消息创建或更新客户
if (!customerRepository.existsByEmail(email)) {
    customerService.createCustomer(new CreateCustomerRequest(...));
}
```
