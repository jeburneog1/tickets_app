package com.nequi.tickets.usecase.port;

import com.nequi.tickets.domain.model.Order;
import reactor.core.publisher.Mono;

public interface CreateOrderPort {
    Mono<Order> execute(String eventId, String customerId, Integer quantity);
}
