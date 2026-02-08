# CustomerController

## 1. 类档案 (Class Profile)
- **功能定义**：客户管理 API，提供客户的 CRUD 操作、访客 Token 生成及特殊角色分配。
- **注解与配置**：
  - `@RestController`: REST API 控制器。
  - `@RequestMapping("/api/v1/customers")`: 基础路径。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `listCustomers` | In: `name`, `channel`, `tag`, `active`<br>Out: `Page<CustomerDto>` | 综合查询客户列表，支持多条件过滤。 | |
| `generateToken` | In: `id`<br>Out: `CustomerTokenResponse` | 为指定客户生成访问 Token（用于 Widget 身份识别）。 | |
| `assignRole` | In: `id`, `roleCode`<br>Out: `CustomerDto` | 分配特殊业务角色（如供应商、物流商）。 | 调用 `SpecialCustomerService` 处理业务逻辑。 |

## 3. 依赖全景 (Dependency Graph)
- **`CustomerService`**: 核心客户管理逻辑。
- **`SpecialCustomerService`**: 特殊客户角色管理。

## 4. 调用指南 (Usage Guide)
```http
POST /api/v1/customers
Content-Type: application/json

{
  "name": "New User",
  "email": "user@test.com"
}
```
