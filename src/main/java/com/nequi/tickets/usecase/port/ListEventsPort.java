package com.nequi.tickets.usecase.port;

import com.nequi.tickets.domain.model.Event;
import reactor.core.publisher.Flux;

public interface ListEventsPort {
    Flux<Event> execute();
}
