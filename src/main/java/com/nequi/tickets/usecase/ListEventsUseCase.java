package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.usecase.port.ListEventsPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ListEventsUseCase implements ListEventsPort {
    
    private final EventRepository eventRepository;
    
    public ListEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    public Flux<Event> execute() {
        return eventRepository.findAll();
    }
    
    public Flux<Event> executeWithAvailability() {
        return eventRepository.findEventsWithAvailability();
    }
    
    public Flux<Event> executeUpcoming() {
        return eventRepository.findUpcomingEvents(java.time.LocalDateTime.now());
    }
}
