package com.nequi.tickets.usecase;

import com.nequi.tickets.config.BusinessProperties;
import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.exception.InsufficientTicketsException;
import com.nequi.tickets.domain.exception.MaxTicketsExceededException;
import com.nequi.tickets.domain.model.Event;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.List;

@Service
public class ReserveTicketsUseCase {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final BusinessProperties businessProperties;

    public ReserveTicketsUseCase(
            EventRepository eventRepository,
            TicketRepository ticketRepository,
            BusinessProperties businessProperties) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.businessProperties = businessProperties;
    }

    public Mono<List<Ticket>> execute(String eventId, String customerId, Integer quantity, String orderId) {

        if (eventId == null || eventId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Event ID is required"));
        }
        if (customerId == null || customerId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Customer ID is required"));
        }
        if (quantity == null || quantity <= 0) {
            return Mono.error(new IllegalArgumentException("Quantity must be positive"));
        }
        int maxTicketsPerOrder = businessProperties.getOrder().getMaxTicketsPerOrder();
        if (quantity > maxTicketsPerOrder) {
            return Mono.error(new MaxTicketsExceededException(
                    quantity, maxTicketsPerOrder
            ));
        }
        if (orderId == null || orderId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Order ID is required"));
        }

        return reserveTicketsWithCreation(eventId, customerId, quantity, orderId, 3);
    }

    private Mono<List<Ticket>> reserveTicketsWithCreation(
            String eventId, 
            String customerId, 
            Integer quantity, 
            String orderId,
            int maxRetries) {

        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with ID: " + eventId)))

                .flatMap(event -> {
                    if (event.availableTickets() < quantity) {
                        return Mono.error(new InsufficientTicketsException(
                                eventId, quantity, event.availableTickets()));
                    }

                    Event updatedEvent = event.reserveTickets(quantity);

                    return eventRepository.save(updatedEvent)

                            .flatMap(savedEvent -> {

                                return ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE)
                                        .take(quantity)
                                        .collectList()
                                        .flatMap(availableTickets -> {

                                            if (availableTickets.size() < quantity) {

                                                Event rollbackEvent = savedEvent.releaseReservedTickets(quantity);
                                                return eventRepository.save(rollbackEvent)
                                                        .then(Mono.error(new InsufficientTicketsException(
                                                                eventId, quantity, availableTickets.size()
                                                        )));
                                            }

                                            List<Ticket> reservedTickets = availableTickets.stream()
                                                    .map(ticket -> ticket.reserveTicket(customerId, orderId))
                                                    .toList();

                                            return ticketRepository.saveAll(reservedTickets)
                                                    .collectList()
                                                    .onErrorResume(error -> {

                                                        Event rollbackEvent = savedEvent.releaseReservedTickets(quantity);

                                                        return eventRepository.save(rollbackEvent)
                                                                .then(Mono.error(new RuntimeException(
                                                                        "Failed to reserve tickets, inventory rolled back", error
                                                                )));
                                                    });
                                        });
                            });
                })

                .retryWhen(Retry.max(maxRetries)
                        .filter(throwable -> throwable instanceof com.nequi.tickets.domain.exception.ConcurrentModificationException));
    }
}
