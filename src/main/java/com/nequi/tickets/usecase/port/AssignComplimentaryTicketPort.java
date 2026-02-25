package com.nequi.tickets.usecase.port;

import com.nequi.tickets.domain.model.Ticket;
import reactor.core.publisher.Mono;

public interface AssignComplimentaryTicketPort {
    Mono<Ticket> execute(String eventId, String customerId, String reason);
}
