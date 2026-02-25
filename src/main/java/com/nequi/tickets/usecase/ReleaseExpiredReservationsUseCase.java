package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReleaseExpiredReservationsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseExpiredReservationsUseCase.class);

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;

    public ReleaseExpiredReservationsUseCase(
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            OrderRepository orderRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
    }

    public Mono<Integer> execute() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("🔍 Searching for reservations expired before: {}", now);

        return ticketRepository.findExpiredReservations(now)
            .collectList()
            .flatMap(expiredTickets -> {

                if (expiredTickets.isEmpty()) {

                    return Mono.just(0);
                }

                List<Ticket> releasedTickets = expiredTickets.stream()
                    .map(ticket -> new Ticket(
                        ticket.ticketId(),
                        ticket.eventId(),
                        TicketStatus.AVAILABLE,
                        null,
                        null,
                        null,
                        null,
                        ticket.version() + 1,
                        ticket.createdAt(),
                        now
                    ))
                    .toList();

                logger.info("🔄 Releasing {} tickets back to AVAILABLE", releasedTickets.size());

                return ticketRepository.saveAll(releasedTickets)

                    .collectList()

                    .flatMap(savedTickets -> {

                        return updateEventInventories(expiredTickets)

                            .then(cancelAffectedOrders(expiredTickets))

                            .thenReturn(savedTickets.size());
                    });
            });
    }

    private Mono<Void> updateEventInventories(List<Ticket> expiredTickets) {

        Map<String, Long> ticketsByEvent = new HashMap<>();
        for (Ticket ticket : expiredTickets) {

            ticketsByEvent.merge(ticket.eventId(), 1L, Long::sum);
        }

        return Mono.whenDelayError(

            ticketsByEvent.entrySet().stream()

                .map(entry -> updateEventInventory(
                    entry.getKey(),
                    entry.getValue().intValue()
                ))
                .toArray(Mono[]::new)
        )

        .then();

    }

    private Mono<Void> updateEventInventory(String eventId, Integer releasedCount) {

        return eventRepository.findById(eventId)
            .flatMap(event -> {

                Integer newAvailable = event.availableTickets() + releasedCount;
                Integer newReserved = Math.max(0, event.reservedTickets() - releasedCount);

                return eventRepository.updateInventory(
                    eventId,
                    event.version(),
                    newAvailable,
                    newReserved
                )
                .then();
            })

            .retry(3)

            .onErrorResume(error -> {

                System.err.println("Failed to update inventory for event " + eventId + ": " + error.getMessage());

                return Mono.empty();
            });
    }

    private Mono<Void> cancelAffectedOrders(List<Ticket> expiredTickets) {

        List<String> orderIds = expiredTickets.stream()
            .map(Ticket::orderId)
            .filter(orderId -> orderId != null && !orderId.isBlank())
            .distinct()
            .toList();

        if (orderIds.isEmpty()) {

            return Mono.empty();
        }

        return orderRepository.findByIds(orderIds)

            .filter(order -> 
                order.status() == OrderStatus.PENDING || 
                order.status() == OrderStatus.PROCESSING
            )

            .flatMap(order -> {

                return orderRepository.save(order.cancel("Reservation expired"));
            })

            .then();
    }
}
