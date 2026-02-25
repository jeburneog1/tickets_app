package com.nequi.tickets.usecase.port;

import com.nequi.tickets.domain.model.Event;
import reactor.core.publisher.Mono;

public interface GetEventPort {
    Mono<Event> execute(String eventId);
}
