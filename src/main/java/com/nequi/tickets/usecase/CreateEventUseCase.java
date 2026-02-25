package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.usecase.port.CreateEventPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class CreateEventUseCase implements CreateEventPort {
    
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    
    public CreateEventUseCase(
            EventRepository eventRepository,
            TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }
    
    @Override
    public Mono<Event> execute(String name, LocalDateTime date, String location, Integer totalCapacity) {
        return Mono.defer(() -> {
            validateInput(name, date, location, totalCapacity);
            String eventId = UUID.randomUUID().toString();
            Event event = Event.create(eventId, name, date, location, totalCapacity);
            
            return eventRepository.save(event)
                    .flatMap(savedEvent -> {
                        List<Ticket> tickets = IntStream.range(0, totalCapacity)
                                .mapToObj(i -> Ticket.createAvailableTicket(
                                        UUID.randomUUID().toString(),
                                        eventId
                                ))
                                .toList();
                        
                        return ticketRepository.saveAll(tickets)
                                .collectList()
                                .thenReturn(savedEvent);
                    });
        });
    }
    
    private void validateInput(String name, LocalDateTime date, String location, Integer totalCapacity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        
        if (date == null) {
            throw new IllegalArgumentException("Event date is required");
        }
        
        if (date.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date must be in the future");
        }
        
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Event location is required");
        }
        
        if (totalCapacity == null || totalCapacity <= 0) {
            throw new IllegalArgumentException("Total capacity must be positive");
        }
    }
}
