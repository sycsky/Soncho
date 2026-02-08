# ChatSessionService

## 1. 类档案 (Class Profile)
- **功能定义**：聊天会话生命周期管理服务。负责会话的创建、分配（路由到客服）、状态流转、成员管理及元数据维护。
- **注解与配置**：
  - `@Service`: 标记为 Spring 服务。
  - `@Transactional(readOnly = true)`: 默认只读事务。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `createSessionForCustomer` | In: `Customer`, `metadata`<br>Out: `ChatSession` | 1. 提取元数据（分类 ID、语言）。<br>2. **客服分配**：调用 `AgentAssignmentStrategy` 分配主责客服和支持客服。<br>3. 初始化会话状态 (`AI_HANDLING`)。<br>4. 序列化元数据并保存。<br>5. **分组分配**：根据分类将会话分配到客服的 SessionGroup。 | 核心业务流程，涉及复杂的分配策略和状态初始化。 |
| `assignSessionToAgentGroup` | (Internal) | 将会话与客服的分组进行绑定，确保客服能在工作台中看到该会话。 | |

## 3. 依赖全景 (Dependency Graph)
- **`AgentAssignmentStrategy`**: 客服分配策略（如轮询、随机、熟客优先）。
- **`SessionGroupService`**: 会话分组管理。
- **`SessionCategoryRepository`**: 分类管理。
- **`ObjectMapper`**: 处理 JSON 元数据。

## 4. 调用指南 (Usage Guide)
```java
// 客户发起新咨询时
Map<String, Object> meta = Map.of("source", "widget", "categoryId", "uuid...");
ChatSession session = chatSessionService.createSessionForCustomer(customer, meta);
```
