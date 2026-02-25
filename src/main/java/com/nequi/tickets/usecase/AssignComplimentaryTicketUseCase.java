package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.exception.InsufficientTicketsException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.usecase.port.AssignComplimentaryTicketPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class AssignComplimentaryTicketUseCase implements AssignComplimentaryTicketPort {
    
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    
    public AssignComplimentaryTicketUseCase(
            EventRepository eventRepository,
            TicketRepository ticketRepository,
            OrderRepository orderRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
    }
    
    @Override
    public Mono<Ticket> execute(String eventId, String customerId, String reason) {
        if (eventId == null || eventId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Event ID is required"));
        }
        if (customerId == null || customerId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Customer ID is required"));
        }
        if (reason == null || reason.isBlank()) {
            return Mono.error(new IllegalArgumentException("Reason is required"));
        }
        
        return eventRepository.findById(eventId)
            .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with ID: " + eventId)))
            .flatMap(event -> {
                if (!event.hasAvailableTickets()) {
                    return Mono.error(new InsufficientTicketsException(
                        eventId, 1, 0
                    ));
                }
                
                return ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)
                    .next()
                    .switchIfEmpty(Mono.error(new InsufficientTicketsException(
                        eventId, 1, 0
                    )))
                    .flatMap(ticket -> {
                        Ticket complimentaryTicket = ticket.assignAsComplimentary(customerId);
                        
                        Event updatedEvent = event.assignComplimentaryTicket(1);
                        
                        String orderId = UUID.randomUUID().toString();
                        Order complimentaryOrder = Order.createComplimentary(
                            orderId,
                            eventId,
                            customerId,
                            List.of(ticket.ticketId())
                        );
                        
                        return ticketRepository.save(complimentaryTicket)
                            .flatMap(savedTicket -> 
                                eventRepository.save(updatedEvent)
                                .then(orderRepository.save(complimentaryOrder))
                                .thenReturn(savedTicket)
                            );
                    });
            })
            .retry(3);
    }
}
