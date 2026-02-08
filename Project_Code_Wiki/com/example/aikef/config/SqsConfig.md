# SqsConfig

## Class Profile
`SqsConfig` configures the AWS SQS (Simple Queue Service) client. This is used for asynchronous messaging, likely for the `SqsDelayService` or other background tasks.

## Method Deep Dive

### `sqsClient()`
- **Logic**:
    - Reads credentials from `aws.sqs.*`.
    - If keys are missing, uses default credential chain (IAM Role).
    - If keys provided, uses static credentials.
    - Builds `SqsClient`.

## Dependency Graph
- AWS SDK (`SqsClient`).

## Source Link
[SqsConfig.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/config/SqsConfig.java)
