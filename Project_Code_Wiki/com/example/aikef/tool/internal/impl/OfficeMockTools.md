# OfficeMockTools

## 1. 类档案 (Class Profile)
- **功能定义**：办公场景模拟内部工具集合，提供任务、审批、会议等可调用能力用于联调测试。
- **注解与配置**：
  - `@Component`
  - 多个 `@Tool` 方法自动注册为内部工具
- **继承/实现**：普通 Spring 组件。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. `createOfficeTask` 创建模拟任务并返回 `TASK-*` 编号。
2. `updateOfficeTaskStatus` 更新任务状态（TODO/IN_PROGRESS/DONE/BLOCKED）。
3. `submitApprovalRequest` 创建审批单并返回 `APR-*` 编号。
4. `processApprovalRequest` 对审批执行 APPROVED/REJECTED。
5. `scheduleOfficeMeeting` 生成模拟会议安排。
6. `listOfficeMockRecords` 查询最近任务或审批记录用于回显验证。

## 3. 依赖全景 (Dependency Graph)
- **LangChain4j Tool 注解体系**：将方法暴露给 Agent 调用。
- **内存存储结构**：通过并发 Map 存储模拟任务与审批记录。

## 4. 调用指南 (Usage Guide)
```java
String task = officeMockTools.createOfficeTask(
        "整理周报",
        "Tom",
        "2026-03-12 18:00",
        "HIGH"
);
```

## 5. 架构师备注 (Architect's Notes)
- 该类定位为测试联调用 mock 工具，不依赖外部办公系统。
- 工具结果可与程序性记忆联动验证“多轮步骤记忆 + 工具执行”链路。
- 关键写操作参数需要用户明确提供，禁止由模型自行臆造默认值。
