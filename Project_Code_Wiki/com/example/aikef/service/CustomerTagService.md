# CustomerTagService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 客户标签管理服务。区分管理 "手动标签"（由客服添加）和 "AI 标签"（由 AI 分析生成），提供增删改查功能。

## 2. Method Deep Dive

### `addManualTag` / `removeManualTag`
- **Signature**: `public CustomerDto addManualTag(UUID customerId, String tag)`
- **Description**: 管理客服手动维护的标签集合。确保标签不重复。

### `addAiTag` / `removeAiTag`
- **Signature**: `public CustomerDto addAiTag(UUID customerId, String tag)`
- **Description**: 管理系统/AI 自动生成的标签集合。

### `setManualTags` / `setAiTags`
- **Signature**: `public CustomerDto setManualTags(UUID customerId, List<String> tags)`
- **Description**: 批量覆盖设置标签列表。

### `getManualTags` / `getAiTags`
- **Signature**: `public List<String> getManualTags(UUID customerId)`
- **Description**: 获取指定类型的标签列表。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `CustomerRepository`: 客户实体存储。
  - `EntityMapper`: DTO 转换。

## 4. Usage Guide
### 场景：用户画像构建
1. **手动标签**：客服在与客户沟通后，手动标记 "价格敏感"、"意向强烈"。
2. **AI 标签**：后台 AI 工作流分析历史聊天记录，自动打上 "高频退货"、"数码爱好者" 标签。
前端展示时，通常将这两类标签分开显示或用不同颜色区分，辅助客服快速了解客户特征。
