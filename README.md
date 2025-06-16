# Amazon Connect Call Summary Recording

## Overview

This Spring Boot application serves as an AWS Lambda function that processes Amazon Connect call center events in real-time. It captures call trace records from Kinesis Data Streams, extracts metadata about calls, recordings, and agents, then stores this information in DynamoDB while managing call recordings in S3.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Amazon Connect │────▶│ Kinesis Stream   │────▶│ Lambda Function │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                           │
                        ┌──────────────────────────────────┴──────────┐
                        │                                             │
                        ▼                                             ▼
                ┌──────────────┐   ┌─────────────┐   ┌──────────────────────┐
                │   DynamoDB   │   │     S3      │   │ Secondary Kinesis    │
                │   (Metadata) │   │ (Recordings)│   │ (Event Forwarding)   │
                └──────────────┘   └──────┬──────┘   └──────────────────────┘
                                          │
                                          ▼
                                    ┌─────────────┐
                                    │ Transcribe  │
                                    │ (Optional)  │
                                    └─────────────┘
```

## Features

- **Real-time Event Processing**: Processes Amazon Connect contact trace records as they stream through Kinesis
- **Call Metadata Extraction**: Captures contact details, agent information, and recording locations
- **DynamoDB Storage**: Persists call metadata for analytics and retrieval
- **Recording Management**: Handles S3 storage locations for call recordings
- **Event Forwarding**: Routes processed events to secondary Kinesis streams
- **Transcription Ready**: Framework for AWS Transcribe integration (currently commented out)
- **Spring Boot Lambda**: Leverages Spring Boot for dependency injection and configuration

## Technical Stack

- **Java 8**: Core programming language
- **Spring Boot 2.5.6**: Application framework
- **AWS Lambda**: Serverless compute platform
- **AWS SDK**: Integration with AWS services
- **Maven**: Build and dependency management

## Prerequisites

- Java 8 or higher
- Maven 3.6+
- AWS Account with appropriate permissions
- AWS CLI configured with credentials

## Environment Variables

Configure these environment variables for the Lambda function:

| Variable | Description | Example |
|----------|-------------|---------|
| `DESTINATION_KINESIS_STREAM` | Target Kinesis stream for forwarding | `connect-events-processed` |
| `APP_REGION` | AWS region | `us-east-1` |
| `RECORDINGS_BUCKET_NAME` | S3 bucket for recordings | `connect-recordings-bucket` |
| `RECORDINGS_KEY_PREFIX` | S3 key prefix for recordings | `recordings/` |
| `RECORDINGS_PUBLIC_READ_ACL` | Make recordings publicly readable | `false` |
| `START_SELECTOR_TYPE` | Kinesis iterator type | `LATEST` |

## Installation & Build

1. Clone the repository:
```bash
git clone https://github.com/Samu-Legal-Technology/amazon-connect-call-summary-recording.git
cd amazon-connect-call-summary-recording
```

2. Build the project:
```bash
mvn clean package
```

3. The Lambda deployment package will be created at:
```
target/amazon-connect-call-summary-recording-0.0.1-SNAPSHOT.jar
```

## Deployment

### Lambda Function Setup

1. Create a new Lambda function in AWS Console
2. Runtime: Java 8 (Corretto)
3. Handler: `com.ott.connect.KinesisVideoStreamingHandler::handleRequest`
4. Memory: 512 MB (recommended)
5. Timeout: 5 minutes
6. Upload the JAR file from target directory

### IAM Permissions

The Lambda execution role requires:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kinesis:DescribeStream",
        "kinesis:GetShardIterator",
        "kinesis:GetRecords",
        "kinesis:ListShards",
        "kinesis:PutRecords"
      ],
      "Resource": "arn:aws:kinesis:*:*:stream/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:Query"
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/OttConnectContactTraceRecord-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::*/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### Kinesis Event Source

Configure the Lambda to be triggered by your Amazon Connect Kinesis stream:
- Event source: Kinesis
- Starting position: Latest
- Batch size: 100 (adjust based on load)

## Data Models

### Contact Trace Record
Contains comprehensive call information including:
- Contact ID and timestamps
- Recording details (location, status)
- Agent information
- Customer endpoint data
- Contact attributes

### Contact Flow Attributes
- Agent extension and name
- System endpoints
- Queue information
- Language preferences
- Voicemail settings

## DynamoDB Tables

### OttConnectContactTraceRecord-Dev
Primary table for storing processed contact records:
- **Partition Key**: ContactId
- **Attributes**: Recording URL, agent details, timestamps, processing status

### dev-ott-connect-contact-details
Secondary table for contact-specific details

## Development

### Running Locally

For local testing, you can simulate Lambda events:

```java
// Create test event
KinesisEvent testEvent = new KinesisEvent();
// Configure test records
new KinesisVideoStreamingHandler().handleRequest(testEvent, null);
```

### Adding New Features

1. **Enable Audio Transcription**:
   - Uncomment `AudioStreamService`
   - Configure AWS Transcribe permissions
   - Update Lambda memory/timeout as needed

2. **Custom Processing**:
   - Extend `KinesisVideoStreamingHandler`
   - Add new service classes in `com.ott.connect.service`
   - Update DynamoDB schema as needed

## Monitoring

- **CloudWatch Logs**: All Lambda executions log to CloudWatch
- **CloudWatch Metrics**: Monitor invocations, errors, duration
- **DynamoDB Metrics**: Track read/write capacity usage
- **Kinesis Metrics**: Monitor stream throughput

## Troubleshooting

### Common Issues

1. **Lambda Timeout**: Increase timeout or optimize batch size
2. **DynamoDB Throttling**: Increase read/write capacity
3. **Kinesis Iterator Age**: Check Lambda concurrency limits
4. **S3 Access Denied**: Verify bucket policies and IAM permissions

### Debug Logging

Enable debug logging by setting:
```java
Logger logger = LoggerFactory.getLogger(YourClass.class);
logger.debug("Debug message");
```

## API Reference

### Lambda Handlers

#### KinesisVideoStreamingHandler
Main entry point for Kinesis events:
```java
public void handleRequest(KinesisEvent kinesisEvent, Context context)
```

#### ConnectLambdaHandler
Alternative handler for direct Connect events:
```java
public Object handleRequest(Object connectRequest, Context context)
```

### Service Classes

#### TranscribeService
Manages audio transcription:
```java
public void transcribeAudio(String recordingUrl, String contactId)
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/NewFeature`)
3. Commit changes (`git commit -m 'Add NewFeature'`)
4. Push to branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

## Security

- All call recordings are stored with server-side encryption in S3
- DynamoDB tables should have encryption at rest enabled
- Use AWS Secrets Manager for sensitive configuration
- Implement least-privilege IAM policies
- Regular security audits of dependencies

## License

Copyright © 2024 Samu Legal Technology. All rights reserved.

## Support

For issues or questions:
- Create a GitHub issue
- Contact the development team
- Check CloudWatch logs for detailed error messages

---

*Maintained by Samu Legal Technology Development Team*