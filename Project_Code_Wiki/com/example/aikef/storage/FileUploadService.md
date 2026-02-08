# FileUploadService

## Class Profile
`FileUploadService` is the high-level service for handling file uploads. It orchestrates validation, storage interaction, and database persistence (`UploadedFile` entity). It supports file categorization, deduplication (optional), and access control.

## Method Deep Dive

### `uploadFile(...)`
- **Logic**:
    1.  **Validation**: Checks file size and MIME type against allowed lists.
    2.  **Categorization**: Determines if it's an image, video, document, etc.
    3.  **Path Generation**: Creates a structured path `category/yyyy/MM/dd/uuid.ext`.
    4.  **Storage**: Delegates to `StorageProvider.upload`.
    5.  **Persistence**: Saves metadata to `UploadedFileRepository`.

### `downloadFile(UUID fileId)`
- **Logic**: Retrieves file metadata from DB and streams content from `StorageProvider`.

### `refreshUrl(UUID fileId)`
- **Logic**: Re-generates the access URL (useful for refreshing expired S3 presigned URLs).

## Dependency Graph
- `StorageProvider`: The backend storage engine.
- `UploadedFileRepository`: DB persistence.

## Usage Guide
Used by `FileController` to handle HTTP upload requests.

```java
UploadedFileDto dto = fileUploadService.uploadFile(multipartFile, userId, "AGENT", null, null, true);
```

## Source Link
[FileUploadService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/storage/FileUploadService.java)
