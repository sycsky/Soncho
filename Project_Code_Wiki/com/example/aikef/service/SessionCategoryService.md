# SessionCategoryService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 会话分类（标签）服务。管理会话的业务分类（如"售前"、"售后"、"投诉"），支持 CRUD 操作及唯一性校验。

## 2. Method Deep Dive

### `createCategory`
- **Signature**: `public SessionCategoryDto createCategory(CreateSessionCategoryRequest request, UUID createdByAgentId)`
- **Description**: 创建新分类。校验名称唯一性。

### `updateCategory`
- **Signature**: `public SessionCategoryDto updateCategory(UUID categoryId, UpdateSessionCategoryRequest request)`
- **Description**: 更新分类属性（名称、颜色、图标等）。

### `deleteCategory`
- **Signature**: `public void deleteCategory(UUID categoryId)`
- **Description**: 删除分类。

### `getAllEnabledCategories`
- **Signature**: `public List<SessionCategoryDto> getAllEnabledCategories()`
- **Description**: 获取所有可用的分类列表，按 `sortOrder` 排序。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `SessionCategoryRepository`: 分类存储。
  - `AgentRepository`: 关联创建人。

## 4. Usage Guide
### 场景：业务统计
运营主管定义了一套分类体系。客服在结束会话时，必须选择一个分类（如 "产品咨询/价格"）。月底时，通过统计各分类的会话数量，可以分析客户关注点分布。
