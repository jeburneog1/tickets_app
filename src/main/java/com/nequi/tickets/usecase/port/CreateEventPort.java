package com.nequi.tickets.usecase.port;

import com.nequi.tickets.domain.model.Event;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface CreateEventPort {
    Mono<Event> execute(String name, LocalDateTime date, String location, Integer totalCapacity);
}
