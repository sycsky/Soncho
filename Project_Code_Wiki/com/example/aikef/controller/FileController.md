# Class Profile: FileController

**File Path**: `com/example/aikef/controller/FileController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Handles file uploads, downloads, and retrieval. It supports both single and batch uploads, associating files with specific business entities (references), and serving files securely or publicly. It abstracts the underlying storage mechanism (Local/S3).

# Method Deep Dive

## Upload
- **`uploadFile(...)`**: Handles `multipart/form-data` upload for a single file. Records uploader info (Agent vs Customer).
- **`uploadFiles(...)`**: Handles batch uploads.

## Retrieval & Download
- **`getFile(id)`**: Gets metadata (name, size, type).
- **`downloadFile(id)`**: Streams the file content with `Content-Disposition: attachment`.
- **`serveFile(...)`**: Serves local files directly for preview (e.g., images), handling content type probing.
- **`getMyFiles(...)`**: Lists files uploaded by the current user.
- **`getFilesByReference(...)`**: Lists files attached to a specific object (e.g., a ticket or message).

## Management
- **`deleteFile(id)`**: Removes the file and its record.
- **`refreshUrl(id)`**: Generates a new access URL (useful for signed URLs in cloud storage).
- **`getUploadConfig()`**: Returns limits like max file size.

# Dependency Graph

**Core Dependencies**:
- `FileUploadService`: Logic for file processing and metadata storage.
- `LocalStorageProvider`: For resolving local file paths (if using local storage).
- `AgentPrincipal` / `CustomerPrincipal`: User identification.

**Key Imports**:
```java
import com.example.aikef.storage.FileUploadService;
import com.example.aikef.storage.LocalStorageProvider;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Uploading an Attachment
`POST /api/v1/files/upload`
- **Headers**: `Authorization: Bearer <token>`
- **Body**: `form-data`
  - `file`: (binary content)
  - `referenceType`: "TICKET"
  - `referenceId`: "uuid..."

# Source Link
[FileController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/FileController.java)
