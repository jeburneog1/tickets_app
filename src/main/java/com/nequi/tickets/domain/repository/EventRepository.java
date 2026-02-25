package com.nequi.tickets.domain.repository;

import com.nequi.tickets.domain.model.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository {
    
    Mono<Event> save(Event event);
    
    Mono<Event> findById(String eventId);
    
    Flux<Event> findAll();
    
    Flux<Event> findUpcomingEvents(java.time.LocalDateTime startDate);
    
    Flux<Event> findEventsWithAvailability();
    
    Mono<Boolean> existsById(String eventId);
    
    Mono<Void> deleteById(String eventId);
    
    Mono<Event> updateInventory(String eventId, Integer expectedVersion, 
                                Integer availableTickets, Integer reservedTickets);
    
    Mono<Event> decrementAvailableTickets(String eventId, Integer expectedVersion, Integer quantity, Integer newAvailable, Integer newReserved);
}
