# UploadedFileDto

## 1. Class Profile
- **Class Name**: `UploadedFileDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers file metadata after upload or when listing files.

## 2. Method Deep Dive
### Fields
- `id` / `originalName` / `url`: Access info.
- `fileSize` / `contentType` / `extension`: Technical info.
- `storageType`: `LOCAL` vs `S3`.
- `uploaderId`: Who uploaded it.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.UploadedFile.FileCategory`

## 4. Usage Guide
Used by `FileUploadController`.
- **Response**: `POST /api/v1/files/upload` returns this DTO.

## 5. Source Link
[UploadedFileDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/UploadedFileDto.java)
