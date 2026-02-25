package com.nequi.tickets.infrastructure.messaging.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.tickets.usecase.ProcessOrderUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.Map;

@Component
public class SQSOrderConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(SQSOrderConsumer.class);
    
    private final SqsAsyncClient sqsClient;
    private final String queueUrl;
    private final ProcessOrderUseCase processOrderUseCase;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxMessagesPerPoll;
    private final int pollWaitTimeSeconds;
    private final int visibilityTimeoutSeconds;
    
    private Disposable consumerDisposable;
    
    public SQSOrderConsumer(
            SqsAsyncClient sqsClient,
            @Value("${aws.sqs.order-processing-queue-url}") String queueUrl,
            @Value("${aws.sqs.consumer.enabled:true}") boolean enabled,
            @Value("${aws.sqs.consumer.max-messages:10}") int maxMessagesPerPoll,
            @Value("${aws.sqs.consumer.wait-time-seconds:20}") int pollWaitTimeSeconds,
            @Value("${aws.sqs.consumer.visibility-timeout-seconds:30}") int visibilityTimeoutSeconds,
            ProcessOrderUseCase processOrderUseCase,
            ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.enabled = enabled;
        this.maxMessagesPerPoll = maxMessagesPerPoll;
        this.pollWaitTimeSeconds = pollWaitTimeSeconds;
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
        this.processOrderUseCase = processOrderUseCase;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("SQS Order Consumer is disabled");
            return;
        }
        
        logger.info("Starting SQS Order Consumer for queue: {}", queueUrl);
        
        consumerDisposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
            .onBackpressureDrop()
            .flatMap(tick -> pollMessages())
            .flatMap(this::processMessage, 5)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                result -> logger.debug("Message processed successfully"),
                error -> logger.error("Error in consumer loop", error),
                () -> logger.info("Consumer completed")
            );
        
        logger.info("SQS Order Consumer started");
    }
    
    @PreDestroy
    public void stop() {
        if (consumerDisposable != null && !consumerDisposable.isDisposed()) {
            logger.info("Stopping SQS Order Consumer");
            consumerDisposable.dispose();
        }
    }
    
    private Flux<Message> pollMessages() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(maxMessagesPerPoll)
            .waitTimeSeconds(pollWaitTimeSeconds)
            .visibilityTimeout(visibilityTimeoutSeconds)
            .messageAttributeNames("All")
            .build();
        
        return Mono.fromCompletionStage(() -> sqsClient.receiveMessage(request))
            .flatMapMany(response -> Flux.fromIterable(response.messages()))
            .doOnNext(message -> logger.debug("Received message from SQS: {}", message.messageId()))
            .onErrorResume(error -> {
                logger.error("Error polling messages from SQS", error);
                return Flux.empty();
            });
    }
    
    private Mono<Void> processMessage(Message message) {
        return Mono.fromCallable(() -> {
                Map<String, String> messageBody = objectMapper.readValue(
                    message.body(), 
                    Map.class
                );
                return messageBody.get("orderId");
            })
            .flatMap(orderId -> {
                logger.info("Processing order from SQS message. Order ID: {}, Message ID: {}", 
                    orderId, message.messageId());
                
                return processOrderUseCase.execute(orderId)
                    .doOnSuccess(order -> 
                        logger.info("Order processed successfully. Order ID: {}, Status: {}", 
                            order.orderId(), order.status()))
                    .doOnError(error -> 
                        logger.error("Failed to process order. Order ID: {}", orderId, error));
            })
            .then(deleteMessage(message))
            .onErrorResume(error -> handleProcessingError(message, error));
    }
    
    private Mono<Void> deleteMessage(Message message) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build();
        
        return Mono.fromCompletionStage(() -> sqsClient.deleteMessage(request))
            .doOnSuccess(response -> 
                logger.debug("Message deleted from SQS. Message ID: {}", message.messageId()))
            .doOnError(error -> 
                logger.error("Failed to delete message from SQS. Message ID: {}", 
                    message.messageId(), error))
            .then();
    }
    
    private Mono<Void> handleProcessingError(Message message, Throwable error) {
        logger.error("Error processing message. Message ID: {}, Error: {}", 
            message.messageId(), error.getMessage());
        
        String receiveCount = message.attributes().getOrDefault(
            MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT.toString(), 
            "1"
        );
        
        logger.warn("Message receive count: {}. Message will retry after visibility timeout or move to DLQ.",
            receiveCount);
        
        return Mono.empty();
    }
}
