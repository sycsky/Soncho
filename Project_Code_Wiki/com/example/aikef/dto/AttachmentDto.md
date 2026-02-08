# AttachmentDto

## 1. Class Profile
- **Class Name**: `AttachmentDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Represents a file attachment in an API response.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier.
- `type`: `IMAGE` or `FILE`.
- `url`: Publicly accessible URL.
- `name`: Filename.
- `sizeKb`: File size in kilobytes.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.AttachmentType`

## 4. Usage Guide
Used in `MessageDto` and `SessionMessageDto`.

## 5. Source Link
[AttachmentDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/AttachmentDto.java)
