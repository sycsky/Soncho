# SpecialCustomerService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 特殊客户管理服务。用于管理具有特定业务角色（如 "VIP"、"黑名单"、"测试用户"）的客户关系。

## 2. Method Deep Dive

### `assignRole`
- **Signature**: `public SpecialCustomer assignRole(UUID customerId, String roleCode)`
- **Description**: 将指定客户标记为特殊角色。
- **Logic**:
  1. 查找客户和角色。
  2. 如果已存在关系，更新角色；否则创建新记录。

### `getCustomersByRole`
- **Signature**: `public List<SpecialCustomer> getCustomersByRole(String roleCode)`
- **Description**: 获取所有属于特定角色的客户列表。

### `createRole`
- **Signature**: `public CustomerRole createRole(String code, String name, String description)`
- **Description**: 定义新的客户角色类型。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `SpecialCustomerRepository`: 关系存储。
  - `CustomerRoleRepository`: 角色定义存储。
  - `CustomerRepository`: 客户存储。

## 4. Usage Guide
### 场景：VIP 服务通道
系统定义了 "VIP" 角色。运营人员通过 `assignRole` 将高净值客户标记为 VIP。后续在 `AiScheduledTaskService` 或路由策略中，可以针对 "VIP" 角色的客户执行差异化的服务逻辑（如优先接入人工、发送专属优惠）。
