package com.nequi.tickets.usecase.port;

import com.nequi.tickets.domain.model.Order;
import reactor.core.publisher.Mono;

public interface ConfirmOrderPort {
    Mono<Order> execute(String orderId);
}
