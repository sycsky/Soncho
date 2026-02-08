# UploadedFile

## 1. Class Profile
- **Class Name**: `UploadedFile`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Asset Entity
- **Purpose**: Records metadata for every file uploaded to the system (images, documents, videos). It abstracts the underlying storage mechanism (S3 vs Local).

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `originalName`: User's filename.
- `storagePath`: Where it is physically stored (e.g., `2023/10/uuid.jpg`).
- `url`: Public or signed URL for access.
- `contentType`: MIME type.
- `fileSize`: Size in bytes.
- `extension`: File suffix.
- `storageType`: `LOCAL` or `S3`.
- `category`: `IMAGE`, `DOCUMENT`, etc.
- `uploaderId` / `uploaderType`: Who uploaded it (Agent, Customer, System).
- `referenceId` / `referenceType`: Logical link (e.g., linked to `Message` ID X).
- `md5Hash`: Deduplication fingerprint.
- `isPublic`: Access control flag.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is managed by `FileUploadService`.
- **Deduplication**: Before uploading, the service checks `findByMd5Hash`. If a match is found, it reuses the existing `UploadedFile` record instead of storing a duplicate.

## 5. Source Link
[UploadedFile.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/UploadedFile.java)
