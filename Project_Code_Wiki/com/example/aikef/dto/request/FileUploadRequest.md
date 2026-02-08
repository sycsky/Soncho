# FileUploadRequest

## 1. Class Profile
- **Class Name**: `FileUploadRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Metadata sent alongside a file upload to link the file to a specific business entity.

## 2. Method Deep Dive
### Fields
- `referenceId`: ID of the related object (e.g., Message ID).
- `referenceType`: Type of the related object (e.g., "MESSAGE", "AVATAR").
- `isPublic`: Whether the file should be accessible without authentication.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `FileController`.
- **Context**: When uploading an image in chat, the frontend sends the file binary + this metadata so the backend knows "this image belongs to Message X".

## 5. Source Link
[FileUploadRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/FileUploadRequest.java)
