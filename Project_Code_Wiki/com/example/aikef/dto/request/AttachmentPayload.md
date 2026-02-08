# AttachmentPayload

## 1. Class Profile
- **Class Name**: `AttachmentPayload`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record`
- **Role**: Data Transfer Object (Request Component)
- **Purpose**: Represents an attachment being sent in a `SendMessageRequest`.

## 2. Method Deep Dive
### Fields
- `type`: `IMAGE`, `FILE`.
- `url`: The file URL (usually returned from a previous upload step).
- `name`: Filename.
- `sizeKb`: Size.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.AttachmentType`

## 4. Usage Guide
Used inside `SendMessageRequest`.
- **Flow**:
  1. Upload file -> Get `url`.
  2. Send message -> Include `AttachmentPayload` with `url`.

## 5. Source Link
[AttachmentPayload.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/AttachmentPayload.java)
