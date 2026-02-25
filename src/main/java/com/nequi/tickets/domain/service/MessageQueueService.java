package com.nequi.tickets.domain.service;

import reactor.core.publisher.Mono;

public interface MessageQueueService {
    
    Mono<Void> sendOrderForProcessing(String orderId);
    
    Mono<Void> sendOrderForProcessing(String orderId, int delaySeconds);
}
