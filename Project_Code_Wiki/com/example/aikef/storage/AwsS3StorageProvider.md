# AwsS3StorageProvider

## Class Profile
`AwsS3StorageProvider` is an implementation of `StorageProvider` that uses AWS S3 (or compatible services like MinIO, Aliyun OSS) for file storage. It is activated when `storage.type=s3`.

## Method Deep Dive

### `init()`
- **Logic**: Initializes the `S3Client` and `S3Presigner` using configured credentials, region, and optional endpoint override.

### `upload(...)`
- **Logic**: Uploads the file stream to the configured S3 bucket using `putObject`. Returns a URL (public or presigned).

### `getUrl(String path)`
- **Logic**:
    - If `storage.s3.public-url` is set (e.g., CloudFront or public bucket), constructs a direct URL.
    - Otherwise, generates a time-limited **Presigned URL** using `S3Presigner`.

## Dependency Graph
- AWS SDK (`S3Client`, `S3Presigner`).

## Usage Guide
Configure in `application.yml`:
```yaml
storage:
  type: s3
  s3:
    access-key: ...
    secret-key: ...
    bucket: my-bucket
    endpoint: http://minio:9000 # Optional
```

## Source Link
[AwsS3StorageProvider.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/storage/AwsS3StorageProvider.java)
