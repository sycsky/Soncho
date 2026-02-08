# QuickReplyDto

## 1. Class Profile
- **Class Name**: `QuickReplyDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers canned response data.

## 2. Method Deep Dive
### Fields
- `id` / `label` / `text`: Content.
- `category`: Grouping.
- `system`: If true, it's a global reply.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `QuickReplyController`.
- **List**: `GET /api/v1/quick-replies`.

## 5. Source Link
[QuickReplyDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/QuickReplyDto.java)
