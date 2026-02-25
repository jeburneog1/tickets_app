package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.EventNotFoundException;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.domain.service.MessageQueueService;
import com.nequi.tickets.usecase.port.CreateOrderPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreateOrderUseCase implements CreateOrderPort {

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final ReserveTicketsUseCase reserveTicketsUseCase;
    private final MessageQueueService messageQueueService;

    public CreateOrderUseCase(
            EventRepository eventRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            ReserveTicketsUseCase reserveTicketsUseCase,
            MessageQueueService messageQueueService) {
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.reserveTicketsUseCase = reserveTicketsUseCase;
        this.messageQueueService = messageQueueService;
    }

    public Mono<Order> execute(String eventId, String customerId, Integer quantity) {
        if (eventId == null || eventId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Event ID is required"));
        }
        if (customerId == null || customerId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Customer ID is required"));
        }
        if (quantity == null || quantity <= 0) {
            return Mono.error(new IllegalArgumentException("Quantity must be positive"));
        }

        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException("Event not found with ID: " + eventId)))

                .flatMap(event -> {
                    String orderId = UUID.randomUUID().toString();
                    
                    return reserveTicketsUseCase.execute(eventId, customerId, quantity, orderId)

                            .flatMap(reservedTickets -> {
                                LocalDateTime now = LocalDateTime.now();

                                List<String> ticketIds = reservedTickets.stream()
                                        .map(Ticket::ticketId)
                                        .toList();

                                Order order = Order.createPending(
                                        orderId,
                                        eventId,
                                        customerId,
                                        ticketIds
                                );

                                return orderRepository.save(order)

                                        .flatMap(savedOrder -> {
                                            List<Ticket> updatedTickets = reservedTickets.stream()
                                                    .map(ticket -> new Ticket(
                                                            ticket.ticketId(),
                                                            ticket.eventId(),
                                                            ticket.status(),
                                                            ticket.customerId(),
                                                            orderId,
                                                            ticket.reservedAt(),
                                                            ticket.reservationExpiresAt(),
                                                            ticket.version() + 1,
                                                            ticket.createdAt(),
                                                            now
                                                    ))
                                                    .toList();

                                            return ticketRepository.saveAll(updatedTickets)
                                                    .collectList()
                                                    .flatMap(updated -> {
                                                        return messageQueueService.sendOrderForProcessing(orderId)
                                                                .thenReturn(savedOrder);
                                                    });
                                        });
                            });
                });
    }
}
