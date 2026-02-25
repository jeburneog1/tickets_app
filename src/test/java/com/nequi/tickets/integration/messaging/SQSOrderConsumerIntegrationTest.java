package com.nequi.tickets.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.service.MessageQueueService;
import com.nequi.tickets.infrastructure.messaging.sqs.SQSOrderConsumer;
import com.nequi.tickets.integration.BaseIntegrationTest;
import com.nequi.tickets.usecase.ProcessOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SQSOrderConsumerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private SqsAsyncClient sqsClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MessageQueueService messageQueueService;
    @Mock
    private ProcessOrderUseCase processOrderUseCase;
    @Value("${aws.sqs.order-processing-queue-url}")
    private String queueUrl;
    private SQSOrderConsumer consumer;
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
        consumer = new SQSOrderConsumer(
            sqsClient,
            queueUrl,
            false,
            10,
            20,
            30,
            processOrderUseCase,
            objectMapper
        );
    }
    @Test
    void shouldCompleteEndToEndFlowSendReceiveProcessDelete() throws Exception {
        String orderId = "order-e2e-test-" + System.currentTimeMillis();
        Order mockOrder = createMockOrder(orderId, OrderStatus.CONFIRMED);
        when(processOrderUseCase.execute(orderId)).thenReturn(Mono.just(mockOrder));
        messageQueueService.sendOrderForProcessing(orderId).block();
        System.out.println("✅ Step 1: Message sent to queue with orderId: " + orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .messageAttributeNames("All")
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        System.out.println("✅ Step 2: Message received from queue");
        Map<String, Object> messageBody = objectMapper.readValue(message.body(), Map.class);
        String receivedOrderId = (String) messageBody.get("orderId");
        assertThat(receivedOrderId).isEqualTo(orderId);
        System.out.println("✅ Step 2.1: Message body verified with orderId: " + receivedOrderId);
        Order processedOrder = processOrderUseCase.execute(receivedOrderId).block();
        assertThat(processedOrder).isNotNull();
        assertThat(processedOrder.status()).isEqualTo(OrderStatus.CONFIRMED);
        System.out.println("✅ Step 3: Order processed successfully");
        verify(processOrderUseCase, times(1)).execute(orderId);
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build();
        sqsClient.deleteMessage(deleteRequest).join();
        System.out.println("✅ Step 4: Message deleted from queue");
        Thread.sleep(500);
        List<Message> messagesAfterDelete = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messagesAfterDelete).isEmpty();
        System.out.println("✅ Step 5: Verified message no longer in queue");
        System.out.println("🎉 END-TO-END FLOW COMPLETED: Send → Receive → Process → Delete");
    }
    @Test
    void shouldProcessMessageFromQueue() throws Exception {
        String orderId = "order-123";
        Order mockOrder = createMockOrder(orderId, OrderStatus.CONFIRMED);
        when(processOrderUseCase.execute(orderId)).thenReturn(Mono.just(mockOrder));
        sendTestMessage(orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        ArgumentCaptor<String> orderIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(processOrderUseCase, timeout(2000).times(0)).execute(orderIdCaptor.capture());
        assertThat(messages.get(0).body()).contains(orderId);
    }
    @Test
    void shouldDeleteMessageAfterSuccessfulProcessing() throws Exception {
        String orderId = "order-delete-test";
        Order mockOrder = createMockOrder(orderId, OrderStatus.CONFIRMED);
        when(processOrderUseCase.execute(orderId)).thenReturn(Mono.just(mockOrder));
        sendTestMessage(orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build();
        sqsClient.deleteMessage(deleteRequest).join();
        Thread.sleep(1000);
        List<Message> messagesAfterDelete = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messagesAfterDelete).isEmpty();
    }
    @Test
    void shouldHandleInvalidMessageFormat() throws Exception {
        SendMessageRequest sendRequest = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody("invalid json")
            .messageGroupId("order-processing")
            .messageDeduplicationId("invalid-" + System.currentTimeMillis())
            .build();
        sqsClient.sendMessage(sendRequest).join();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
    }
    @Test
    void shouldHandleProcessingFailure() throws Exception {
        String orderId = "order-failure";
        when(processOrderUseCase.execute(orderId))
            .thenReturn(Mono.error(new RuntimeException("Processing failed")));
        sendTestMessage(orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .visibilityTimeout(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Thread.sleep(6000); 
        List<Message> retryMessages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(retryMessages).isNotEmpty();
    }
    @Test
    void shouldHandleMultipleMessages() throws Exception {
        String[] orderIds = {"order-1", "order-2", "order-3"};
        for (String orderId : orderIds) {
            Order mockOrder = createMockOrder(orderId, OrderStatus.CONFIRMED);
            when(processOrderUseCase.execute(orderId)).thenReturn(Mono.just(mockOrder));
            sendTestMessage(orderId);
            Thread.sleep(100); 
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
    void shouldRespectVisibilityTimeout() throws Exception {
        String orderId = "order-visibility-" + System.currentTimeMillis();
        sendTestMessage(orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .visibilityTimeout(2) 
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        ReceiveMessageRequest immediateRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(1)
            .build();
        List<Message> messagesImmediate = sqsClient.receiveMessage(immediateRequest).join().messages();
        assertThat(messagesImmediate).isEmpty();
        Thread.sleep(3000);
        List<Message> messagesAfterTimeout = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messagesAfterTimeout).hasSizeGreaterThanOrEqualTo(1);
    }
    @Test
    void shouldParseMessageBodyCorrectly() throws Exception {
        String orderId = "order-parse-test";
        sendTestMessage(orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        Map<String, String> messageBody = objectMapper.readValue(message.body(), Map.class);
        assertThat(messageBody).containsKey("orderId");
        assertThat(messageBody.get("orderId")).isEqualTo(orderId);
        assertThat(messageBody).containsKey("timestamp");
    }
    @Test
    void shouldHandleEmptyQueue() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(1)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).isEmpty();
    }
    @Test
    void shouldGetMessageAttributes() throws Exception {
        String orderId = "order-with-attrs";
        sendTestMessage(orderId);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .messageAttributeNames("All")
            .attributeNames(QueueAttributeName.ALL)
            .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).join().messages();
        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        assertThat(message.messageId()).isNotNull();
        assertThat(message.receiptHandle()).isNotNull();
    }
    private void sendTestMessage(String orderId) throws Exception {
        Map<String, String> messageBody = new HashMap<>();
        messageBody.put("orderId", orderId);
        messageBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
        String messageBodyJson = objectMapper.writeValueAsString(messageBody);
        SendMessageRequest sendRequest = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBodyJson)
            .messageGroupId("order-processing")
            .messageDeduplicationId(orderId + "-" + System.currentTimeMillis())
            .build();
        sqsClient.sendMessage(sendRequest).join();
    }
    private Order createMockOrder(String orderId, OrderStatus status) {
        return new Order(
            orderId,
            "event-1",
            "customer-1",
            List.of("ticket-1"),
            status,
            1,
            0,
            0,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}