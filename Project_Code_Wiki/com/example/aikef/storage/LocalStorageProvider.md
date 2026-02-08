# LocalStorageProvider

## Class Profile
`LocalStorageProvider` is the default implementation of `StorageProvider` that stores files on the local server filesystem. It is active when `storage.type=local` (or missing).

## Method Deep Dive

### `init()`
- **Logic**: Creates the base upload directory if it doesn't exist.

### `upload(...)`
- **Logic**: Copies the input stream to a file in the `storage.local.base-path` directory.

### `getUrl(String path)`
- **Logic**: Returns a URL pointing to the local static resource handler (e.g., `http://localhost:8080/api/v1/files/...`).

## Dependency Graph
- Java NIO (`Files`, `Paths`).

## Usage Guide
Good for development or simple deployments without cloud storage.

## Source Link
[LocalStorageProvider.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/storage/LocalStorageProvider.java)
