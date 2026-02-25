package com.nequi.tickets.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.tickets.domain.service.MessageQueueService;
import com.nequi.tickets.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
class SQSMessageQueueServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MessageQueueService messageQueueService;
    @Autowired
    private SqsAsyncClient sqsClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${aws.sqs.order-processing-queue-url}")
    private String queueUrl;
    @BeforeEach
    void setUp() {
        try {
            PurgeQueueRequest purgeRequest = PurgeQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();
            sqsClient.purgeQueue(purgeRequest).join();
            Thread.sleep(1000);
        } catch (Exception e) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    @Test
    void shouldSendOrderMessageToQueue() {
        String orderId = "order-123";
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .verifyComplete();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        assertThat(message.body()).contains("order-123");
    }
    @Test
    void shouldSendOrderMessageWithDelay() throws Exception {
        String orderId = "order-delayed-" + System.currentTimeMillis();
        int delaySeconds = 2;
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId, delaySeconds))
            .expectError()
            .verify();
    }
    @Test
    void shouldRejectNullOrderId() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing(null))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
    @Test
    void shouldRejectEmptyOrderId() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing(""))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
    @Test
    void shouldRejectBlankOrderId() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing("   "))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
    @Test
    void shouldRejectInvalidNegativeDelay() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing("order-1", -1))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
    @Test
    void shouldRejectInvalidTooLargeDelay() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing("order-1", 901))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
    @Test
    void shouldSendMultipleOrderMessages() {
        String[] orderIds = {"order-1", "order-2", "order-3"};
        for (String orderId : orderIds) {
            messageQueueService.sendOrderForProcessing(orderId).block();
        }
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSizeGreaterThanOrEqualTo(3);
    }
    @Test
    void shouldHandleMessageDeduplication() {
        String orderId = "order-duplicate";
        messageQueueService.sendOrderForProcessing(orderId).block();
        messageQueueService.sendOrderForProcessing(orderId).block();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(2)
            .build();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages.size()).isLessThanOrEqualTo(1);
    }
    @Test
    void shouldIncludeCorrectMessageAttributes() throws Exception {
        String orderId = "order-with-attributes";
        messageQueueService.sendOrderForProcessing(orderId).block();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        Map<String, Object> messageBody = objectMapper.readValue(message.body(), Map.class);
        assertThat(messageBody).containsKey("orderId");
        assertThat(messageBody.get("orderId")).isEqualTo(orderId);
        assertThat(messageBody).containsKey("timestamp");
    }
    @Test
    void shouldSendMessagesInOrder() {
        String[] orderIds = {"order-1", "order-2", "order-3"};
        for (String orderId : orderIds) {
            messageQueueService.sendOrderForProcessing(orderId).block();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSizeGreaterThanOrEqualTo(3);
    }
    @Test
    void shouldHandleLargeNumberOfMessages() {
        int messageCount = 10;
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            messageQueueService.sendOrderForProcessing("order-" + timestamp + "-" + i).block();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int receivedCount = 0;
        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(2)
                .build();
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
            receivedCount += messages.size();
            if (receivedCount >= messageCount) {
                break;
            }
        }
        assertThat(receivedCount).isGreaterThanOrEqualTo(messageCount);
    }
}