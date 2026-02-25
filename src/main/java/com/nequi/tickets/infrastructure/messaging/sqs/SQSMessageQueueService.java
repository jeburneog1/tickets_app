package com.nequi.tickets.infrastructure.messaging.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nequi.tickets.domain.service.MessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class SQSMessageQueueService implements MessageQueueService {
    
    private static final Logger logger = LoggerFactory.getLogger(SQSMessageQueueService.class);
    
    private final SqsAsyncClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    
    public SQSMessageQueueService(
            SqsAsyncClient sqsClient,
            @Value("${aws.sqs.order-processing-queue-url}") String queueUrl,
            ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Mono<Void> sendOrderForProcessing(String orderId) {
        return sendOrderForProcessing(orderId, 0);
    }
    
    @Override
    public Mono<Void> sendOrderForProcessing(String orderId, int delaySeconds) {
        if (orderId == null || orderId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Order ID cannot be null or empty"));
        }
        
        if (delaySeconds < 0 || delaySeconds > 900) {
            return Mono.error(new IllegalArgumentException("Delay must be between 0 and 900 seconds"));
        }
        
        try {
            Map<String, String> messageBody = new HashMap<>();
            messageBody.put("orderId", orderId);
            messageBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            String messageBodyJson = objectMapper.writeValueAsString(messageBody);
            
            SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBodyJson)
                .delaySeconds(delaySeconds)
                .messageGroupId("order-processing")
                .messageDeduplicationId(orderId)
                .build();
            
            return Mono.fromCompletionStage(() -> sqsClient.sendMessage(request))
                .doOnSuccess(response -> 
                    logger.info("Order message sent to SQS. Order ID: {}, Message ID: {}, Delay: {}s",
                        orderId, response.messageId(), delaySeconds))
                .doOnError(error -> 
                    logger.error("Failed to send order message to SQS. Order ID: {}", orderId, error))
                .then();
                
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order message. Order ID: {}", orderId, e);
            return Mono.error(new RuntimeException("Failed to serialize message", e));
        }
    }
}
