# BaseWorkflowNode

## 1. 类档案 (Class Profile)
- **功能定义**：工作流节点的抽象基类，封装了所有节点通用的上下文访问、配置读取、执行日志记录等基础功能。
- **注解与配置**：
  - `public abstract class BaseWorkflowNode extends NodeComponent`: 继承自 LiteFlow 的 `NodeComponent`，作为所有自定义节点的父类。
- **继承/实现**：
  - 继承 `NodeComponent` (LiteFlow)。
  - 被 `StartNode`, `LlmNode`, `ApiNode` 等具体业务节点继承。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `getWorkflowContext` | Out: `WorkflowContext` | 从 LiteFlow 上下文中获取 `WorkflowContext` Bean。 | 强类型获取，方便子类使用。 |
| `getActualNodeId` | Out: `String` | 1. 获取当前 LiteFlow 节点的 tag。<br>2. 验证 tag 是否在节点配置中存在。<br>3. 如果存在则返回 tag（作为 ReactFlow 节点 ID），否则返回默认 nodeId。 | 解决 ReactFlow 节点 ID 与 LiteFlow 组件 ID 的映射问题。 |
| `getNodeConfig` | Out: `JsonNode` | 通过 `getActualNodeId()` 获取当前节点的 JSON 配置对象。 | 统一配置获取入口。 |
| `getConfigString/Int/Boolean` | In: `key`, `defaultValue`<br>Out: `T` | 安全地从 `JsonNode` 中读取指定类型的配置值，支持默认值。 | 包含空值检查和类型转换。 |
| `recordExecution` | In: `ctx`, `nodeId`, `input`, `output`, `success`, `error` | 创建 `NodeExecutionDetail` 对象并添加到上下文的执行详情列表中。 | 记录节点执行耗时、输入输出、成功状态，用于前端展示和审计。 |
| `renderTemplate` | In: `template`<br>Out: `String` | 解析字符串中的 `{{variable}}` 模板，替换为上下文中的实际值。 | 支持 `sys.query`, `sys.lastOutput` 及自定义变量 `var.xxx`。 |

## 3. 依赖全景 (Dependency Graph)
- **`WorkflowContext`**: 核心依赖，通过它获取流程状态和配置。
- **`TemplateEngine`**: 用于变量替换和模板渲染。
- **`Jackson (JsonNode)`**: 用于处理节点配置 JSON。

## 4. 调用指南 (Usage Guide)
*此为抽象类，无法直接实例化。需继承此类实现具体节点逻辑。*

**子类实现示例**：
```java
@LiteflowComponent("myNode")
public class MyNode extends BaseWorkflowNode {
    @Override
    public void process() {
        // 1. 获取配置
        String param = getConfigString("paramName", "default");
        
        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 3. 执行逻辑
        String result = "Processed " + param;
        
        // 4. 设置输出
        setOutput(result);
        
        // 5. 记录执行日志
        recordExecution(getWorkflowContext(), getActualNodeId(), "myType", getName(), 
            param, result, startTime, true, null);
    }
}
```
