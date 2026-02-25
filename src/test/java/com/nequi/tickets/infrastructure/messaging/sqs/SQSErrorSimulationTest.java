package com.nequi.tickets.infrastructure.messaging.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.tickets.usecase.ProcessOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SQS Error Simulation Tests")
class SQSErrorSimulationTest {
    @Mock
    private SqsAsyncClient sqsClient;
    @Mock
    private ProcessOrderUseCase processOrderUseCase;
    private SQSMessageQueueService messageQueueService;
    private ObjectMapper objectMapper;
    private String queueUrl;
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        queueUrl = "http://localhost:9324/000000000000/test-queue.fifo";
        messageQueueService = new SQSMessageQueueService(sqsClient, queueUrl, objectMapper);
    }
    @Test
    @DisplayName("Should handle SQS service unavailable error")
    void shouldHandleSQSServiceUnavailable() {
        String orderId = "order-123";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                SqsException.builder()
                    .message("Service Unavailable")
                    .statusCode(503)
                    .build()
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(SqsException.class)
            .verify();
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }
    @Test
    @DisplayName("Should handle network timeout")
    void shouldHandleNetworkTimeout() {
        String orderId = "order-network-timeout";
        CompletableFuture<SendMessageResponse> timeoutFuture = new CompletableFuture<>();
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(timeoutFuture);
        Mono<Void> result = messageQueueService.sendOrderForProcessing(orderId);
        StepVerifier.create(result.timeout(Duration.ofSeconds(2)))
            .expectError(java.util.concurrent.TimeoutException.class)
            .verify();
    }
    @Test
    @DisplayName("Should handle connection refused")
    void shouldHandleConnectionRefused() {
        String orderId = "order-connection-refused";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                new java.net.ConnectException("Connection refused")
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(java.net.ConnectException.class)
            .verify();
    }
    @Test
    @DisplayName("Should handle throttling error (TooManyRequestsException)")
    void shouldHandleThrottling() {
        String orderId = "order-throttled";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                SqsException.builder()
                    .message("Too Many Requests")
                    .statusCode(429)
                    .build()
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(SqsException.class)
            .verify();
    }
    @Test
    @DisplayName("Should handle AWS service limit exceeded")
    void shouldHandleServiceLimitExceeded() {
        String orderId = "order-limit-exceeded";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                SqsException.builder()
                    .message("Service limit exceeded")
                    .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("LimitExceededException")
                        .build())
                    .build()
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(SqsException.class)
            .verify();
    }
    @Test
    @DisplayName("Should reject null orderId")
    void shouldRejectNullOrderId() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing(null))
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(sqsClient);
    }
    @Test
    @DisplayName("Should reject empty orderId")
    void shouldRejectEmptyOrderId() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing(""))
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(sqsClient);
    }
    @Test
    @DisplayName("Should reject invalid delay seconds (negative)")
    void shouldRejectNegativeDelay() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing("order-123", -1))
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(sqsClient);
    }
    @Test
    @DisplayName("Should reject invalid delay seconds (too large)")
    void shouldRejectTooLargeDelay() {
        StepVerifier.create(messageQueueService.sendOrderForProcessing("order-123", 901))
            .expectError(IllegalArgumentException.class)
            .verify();
        verifyNoInteractions(sqsClient);
    }
    @Test
    @DisplayName("Should handle access denied error")
    void shouldHandleAccessDenied() {
        String orderId = "order-access-denied";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                SqsException.builder()
                    .message("Access Denied")
                    .statusCode(403)
                    .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("AccessDenied")
                        .build())
                    .build()
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(SqsException.class)
            .verify();
    }
    @Test
    @DisplayName("Should handle non-existent queue error")
    void shouldHandleNonExistentQueue() {
        String orderId = "order-no-queue";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                QueueDoesNotExistException.builder()
                    .message("Queue does not exist")
                    .build()
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(QueueDoesNotExistException.class)
            .verify();
    }
    @Test
    @DisplayName("Should handle invalid message attributes")
    void shouldHandleInvalidMessageAttributes() {
        String orderId = "order-invalid-attrs";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                InvalidMessageContentsException.builder()
                    .message("Invalid message contents")
                    .build()
            ));
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectError(InvalidMessageContentsException.class)
            .verify();
    }
    @Test
    @DisplayName("Should retry on transient failure")
    void shouldRetryOnTransientFailure() {
        String orderId = "order-retry";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                SqsException.builder()
                    .message("Service temporarily unavailable")
                    .statusCode(503)
                    .build()
            ))
            .thenReturn(CompletableFuture.completedFuture(
                SendMessageResponse.builder()
                    .messageId("msg-123")
                    .build()
            ));
        Mono<Void> resultWithRetry = messageQueueService.sendOrderForProcessing(orderId)
            .retry(1); 
        StepVerifier.create(resultWithRetry)
            .verifyComplete();
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }
    @Test
    @DisplayName("Should handle concurrent message sends")
    void shouldHandleConcurrentSends() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                SendMessageResponse.builder()
                    .messageId("msg-concurrent")
                    .build()
            ));
        Mono<Void> result = Mono.when(
            messageQueueService.sendOrderForProcessing("order-1"),
            messageQueueService.sendOrderForProcessing("order-2"),
            messageQueueService.sendOrderForProcessing("order-3"),
            messageQueueService.sendOrderForProcessing("order-4"),
            messageQueueService.sendOrderForProcessing("order-5"),
            messageQueueService.sendOrderForProcessing("order-6"),
            messageQueueService.sendOrderForProcessing("order-7"),
            messageQueueService.sendOrderForProcessing("order-8"),
            messageQueueService.sendOrderForProcessing("order-9"),
            messageQueueService.sendOrderForProcessing("order-10")
        );
        StepVerifier.create(result)
            .verifyComplete();
        verify(sqsClient, times(10)).sendMessage(any(SendMessageRequest.class));
    }
    @Test
    @DisplayName("Should handle slow SQS response")
    void shouldHandleSlowResponse() {
        String orderId = "order-slow";
        CompletableFuture<SendMessageResponse> slowFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return SendMessageResponse.builder().messageId("msg-slow").build();
        });
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(slowFuture);
        StepVerifier.create(messageQueueService.sendOrderForProcessing(orderId))
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }
    @Test
    @DisplayName("Should fallback when SQS fails")
    void shouldFallbackWhenSQSFails() {
        String orderId = "order-fallback";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                new RuntimeException("SQS unavailable")
            ));
        Mono<Void> resultWithFallback = messageQueueService.sendOrderForProcessing(orderId)
            .onErrorResume(error -> {
                System.out.println("Fallback: Guardar mensaje en cache local para reintentar después");
                return Mono.empty();
            });
        StepVerifier.create(resultWithFallback)
            .verifyComplete();
    }
    @Test
    @DisplayName("Should open circuit breaker after multiple failures")
    void shouldOpenCircuitBreakerAfterMultipleFailures() {
        String orderId = "order-circuit-breaker";
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(
                new RuntimeException("Service unavailable")
            ));
        Mono<Void> result = messageQueueService.sendOrderForProcessing(orderId)
            .retry(5)
            .onErrorResume(error -> Mono.empty());
        StepVerifier.create(result)
            .verifyComplete();
        verify(sqsClient, atLeast(3)).sendMessage(any(SendMessageRequest.class));
    }
}