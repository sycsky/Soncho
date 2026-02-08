# Attachment

## 1. Class Profile
- **Class Name**: `Attachment`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Asset Entity
- **Purpose**: Represents a file attached to a chat message. It links a specific message to a file URL and metadata.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `message`: The chat message this attachment belongs to.
- `type`: `IMAGE`, `FILE`, etc.
- `url`: The physical location of the file.
- `name`: Display name of the file.
- `sizeKb`: File size in kilobytes.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Message`: The parent message.
  - `com.example.aikef.model.enums.AttachmentType`: Type enum.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is managed by `MessageService`.
- **Creation**: When a message is sent with files, `Attachment` records are created and linked to the `Message` ID.
- **Rendering**: The frontend uses the `url` and `type` to decide how to render the attachment (e.g., showing an image preview or a download button).

## 5. Source Link
[Attachment.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/Attachment.java)
