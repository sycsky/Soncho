# UploadedFileRepository

## 1. Class Profile
- **Class Name**: `UploadedFileRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `UploadedFile` entities.

## 2. Method Deep Dive
### Query Methods
- `findByMd5Hash(String md5Hash)`: For deduplication.
- `findByStoragePath(String path)`: Lookup by physical location.
- `findByUploaderIdAndCategory(...)`: Filtered list for "My Files" UI.
- `findByReferenceIdAndReferenceType(...)`: Find all files attached to a specific message or ticket.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.UploadedFile`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `FileUploadService` and `FileController`.
- **Cleanup**: Periodic tasks can find orphaned files (files with no `referenceId`) and delete them from storage.

## 5. Source Link
[UploadedFileRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/UploadedFileRepository.java)
