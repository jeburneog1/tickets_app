package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class CheckAvailabilityUseCase {
    
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    
    public CheckAvailabilityUseCase(EventRepository eventRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }
    
    public Mono<Integer> execute(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Event ID is required"));
        }
        
        return eventRepository.findById(eventId)
            .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with ID: " + eventId)))
            .flatMap(event -> {
                return ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)
                    .map(Long::intValue);
            });
    }
    
    public Mono<AvailabilityDetails> executeDetailed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Event ID is required"));
        }
        
        return eventRepository.findById(eventId)
            .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with ID: " + eventId)))
            .flatMap(event -> {
                Mono<Long> availableCount = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.AVAILABLE);
                Mono<Long> reservedCount = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.RESERVED);
                Mono<Long> soldCount = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.SOLD);
                
                return Mono.zip(availableCount, reservedCount, soldCount)
                    .map(tuple -> new AvailabilityDetails(
                        event.totalCapacity(),
                        tuple.getT1().intValue(),
                        tuple.getT2().intValue(),
                        tuple.getT3().intValue(),
                        LocalDateTime.now()
                    ));
            });
    }
    
    public record AvailabilityDetails(
        Integer totalCapacity,
        Integer availableTickets,
        Integer reservedTickets,
        Integer soldTickets,
        LocalDateTime checkedAt
    ) {}
}
