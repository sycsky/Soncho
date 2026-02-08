# StorageProvider

## Class Profile
`StorageProvider` is the strategy interface for file storage. It abstracts the underlying storage mechanism (Local vs S3), allowing the application to switch providers via configuration.

## Methods
- `getType()`: Returns provider type (LOCAL, S3).
- `upload(...)`: Stores file and returns a URL.
- `download(...)`: Retrieves file content stream.
- `delete(...)`: Removes file.
- `exists(...)`: Checks existence.
- `getUrl(...)`: Generates access URL.

## Implementations
- `LocalStorageProvider`
- `AwsS3StorageProvider`

## Source Link
[StorageProvider.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/storage/StorageProvider.java)
