package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.usecase.port.GetEventPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetEventUseCase implements GetEventPort {
    
    private final EventRepository eventRepository;
    
    public GetEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    public Mono<Event> execute(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Event ID is required"));
        }
        
        return eventRepository.findById(eventId)
            .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with ID: " + eventId)));
    }
}
