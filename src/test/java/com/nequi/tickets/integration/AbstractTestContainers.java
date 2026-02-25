package com.nequi.tickets.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
public abstract class AbstractTestContainers {
    private static final String ACCESS_KEY = "test";
    private static final String SECRET_KEY = "test";
    private static final Region AWS_REGION = Region.US_EAST_1;
    protected static final LocalStackContainer localStack;
    static {
        localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(DYNAMODB, SQS)
            .withEnv("DEFAULT_REGION", AWS_REGION.id())
            .withReuse(true);
        localStack.start();
    }
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint", 
            () -> localStack.getEndpointOverride(DYNAMODB).toString());
        registry.add("aws.dynamodb.region", () -> AWS_REGION.id());
        registry.add("aws.dynamodb.accessKey", () -> ACCESS_KEY);
        registry.add("aws.dynamodb.secretKey", () -> SECRET_KEY);
        registry.add("aws.sqs.endpoint", 
            () -> localStack.getEndpointOverride(SQS).toString());
        registry.add("aws.sqs.region", () -> AWS_REGION.id());
        registry.add("aws.sqs.accessKey", () -> ACCESS_KEY);
        registry.add("aws.sqs.secretKey", () -> SECRET_KEY);
        String queueUrl = localStack.getEndpointOverride(SQS).toString() + "/000000000000/order-processing-test.fifo";
        registry.add("aws.sqs.order-processing-queue-url", () -> queueUrl);
    }
    @BeforeAll
    static void initializeInfrastructure() {
        System.out.println("🚀 Initializing TestContainers infrastructure...");
        System.out.println("📍 DynamoDB endpoint: " + localStack.getEndpointOverride(DYNAMODB));
        System.out.println("📍 SQS endpoint: " + localStack.getEndpointOverride(SQS));
        try {
            createDynamoDbTables();
            createSqsQueues();
            System.out.println("✅ TestContainers infrastructure ready!");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize infrastructure: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize test infrastructure", e);
        }
    }
    private static void createDynamoDbTables() {
        System.out.println("📊 Creating DynamoDB tables...");
        DynamoDbAsyncClient dynamoDbClient = DynamoDbAsyncClient.builder()
            .endpointOverride(localStack.getEndpointOverride(DYNAMODB))
            .region(AWS_REGION)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
            .build();
        try {
            createEventsTable(dynamoDbClient).join();
            System.out.println("✅ Events table created");
            createTicketsTable(dynamoDbClient).join();
            System.out.println("✅ Tickets table created");
            createOrdersTable(dynamoDbClient).join();
            System.out.println("✅ Orders table created");
        } finally {
            dynamoDbClient.close();
        }
    }
    private static CompletableFuture<Void> createEventsTable(DynamoDbAsyncClient client) {
        CreateTableRequest request = CreateTableRequest.builder()
            .tableName("events")
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("eventId")
                    .keyType(KeyType.HASH)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("eventId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("date")
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName("DateIndex")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("date")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                    .build()
            )
            .provisionedThroughput(ProvisionedThroughput.builder()
                .readCapacityUnits(5L)
                .writeCapacityUnits(5L)
                .build())
            .build();
        return client.createTable(request)
            .thenApply(response -> (Void) null)
            .exceptionally(ex -> {
                if (!ex.getMessage().contains("Table already exists")) {
                    throw new RuntimeException("Failed to create events table", ex);
                }
                return null;
            });
    }
    private static CompletableFuture<Void> createTicketsTable(DynamoDbAsyncClient client) {
        CreateTableRequest request = CreateTableRequest.builder()
            .tableName("tickets")
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("ticketId")
                    .keyType(KeyType.HASH)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("ticketId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("eventId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("status")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("customerId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("orderId")
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName("eventId-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("eventId")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName("eventId-status-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("eventId")
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement.builder()
                            .attributeName("status")
                            .keyType(KeyType.RANGE)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName("customerId-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("customerId")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName("orderId-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("orderId")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                    .build()
            )
            .provisionedThroughput(ProvisionedThroughput.builder()
                .readCapacityUnits(10L)
                .writeCapacityUnits(10L)
                .build())
            .build();
        return client.createTable(request)
            .thenApply(response -> (Void) null)
            .exceptionally(ex -> {
                if (!ex.getMessage().contains("Table already exists")) {
                    throw new RuntimeException("Failed to create tickets table", ex);
                }
                return null;
            });
    }
    private static CompletableFuture<Void> createOrdersTable(DynamoDbAsyncClient client) {
        CreateTableRequest request = CreateTableRequest.builder()
            .tableName("orders")
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("orderId")
                    .keyType(KeyType.HASH)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("orderId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("customerId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("eventId")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("status")
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName("customerId-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("customerId")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName("eventId-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("eventId")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName("status-index")
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("status")
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                    .build()
            )
            .provisionedThroughput(ProvisionedThroughput.builder()
                .readCapacityUnits(5L)
                .writeCapacityUnits(5L)
                .build())
            .build();
        return client.createTable(request)
            .thenApply(response -> (Void) null)
            .exceptionally(ex -> {
                if (!ex.getMessage().contains("Table already exists")) {
                    throw new RuntimeException("Failed to create orders table", ex);
                }
                return null;
            });
    }
    private static void createSqsQueues() {
        System.out.println("📬 Creating SQS queues...");
        SqsAsyncClient sqsClient = SqsAsyncClient.builder()
            .endpointOverride(localStack.getEndpointOverride(SQS))
            .region(AWS_REGION)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
            .build();
        try {
            String dlqUrl = createQueue(sqsClient, "order-processing-dlq.fifo", true).join();
            System.out.println("✅ DLQ created: order-processing-dlq.fifo at " + dlqUrl);
            String mainQueueUrl = createQueue(sqsClient, "order-processing-test.fifo", true).join();
            System.out.println("✅ Main queue created: order-processing-test.fifo at " + mainQueueUrl);
        } finally {
            sqsClient.close();
        }
    }
    private static CompletableFuture<String> createQueue(SqsAsyncClient client, String queueName, boolean isFifo) {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        if (isFifo) {
            attributes.put(QueueAttributeName.FIFO_QUEUE, "true");
            attributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false");
        }
        attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, "345600"); 
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "300"); 
        CreateQueueRequest request = CreateQueueRequest.builder()
            .queueName(queueName)
            .attributes(attributes)
            .build();
        return client.createQueue(request)
            .thenApply(response -> response.queueUrl())
            .exceptionally(ex -> {
                if (!ex.getMessage().contains("QueueAlreadyExists")) {
                    System.err.println("⚠️  Warning creating queue " + queueName + ": " + ex.getMessage());
                }
                return null;
            });
    }
}