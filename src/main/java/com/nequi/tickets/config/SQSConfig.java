package com.nequi.tickets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
public class SQSConfig {

    @Value("${aws.sqs.endpoint:}")
    private String sqsEndpoint;

    @Value("${aws.sqs.region}")
    private String region;

    @Value("${aws.sqs.access-key}")
    private String accessKey;

    @Value("${aws.sqs.secret-key}")
    private String secretKey;

    @Value("${aws.sqs.queue.order-processing}")
    private String orderProcessingQueueName;

    @Value("${aws.sqs.queue.order-processing-dlq}")
    private String orderProcessingDlqName;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        var clientBuilder = SqsAsyncClient.builder()
                .region(Region.of(region));

        clientBuilder.credentialsProvider(
                StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                )
        );

        if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
            clientBuilder.endpointOverride(URI.create(sqsEndpoint));
        }

        return clientBuilder.build();
    }

    @Bean("orderProcessingQueueName")
    public String orderProcessingQueueName() {
        return orderProcessingQueueName;
    }

    @Bean("orderProcessingDlqName")
    public String orderProcessingDlqName() {
        return orderProcessingDlqName;
    }
}
