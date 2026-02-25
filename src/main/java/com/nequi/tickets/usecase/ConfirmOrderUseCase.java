package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.InvalidStateTransitionException;
import com.nequi.tickets.domain.exception.OrderNotFoundException;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import com.nequi.tickets.usecase.port.ConfirmOrderPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConfirmOrderUseCase implements ConfirmOrderPort {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    
    public ConfirmOrderUseCase(
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            EventRepository eventRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }
    
    public Mono<Order> execute(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Order ID is required"));
        }
        
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId)))
            .flatMap(order -> {
                if (order.status() == OrderStatus.CONFIRMED) {
                    return Mono.just(order);
                }
                
                if (order.status() != OrderStatus.PROCESSING) {
                    return Mono.error(new InvalidStateTransitionException(
                        "Order",
                        order.orderId(),
                        order.status().toString(),
                        "CONFIRM (requires PROCESSING state)"
                    ));
                }
                
                return confirmTickets(order)
                    .flatMap(confirmedOrder -> updateEventInventory(confirmedOrder))
                    .flatMap(this::confirmOrder);
            });
    }
    
    private Mono<Order> confirmTickets(Order order) {
        return ticketRepository.findByOrderId(order.orderId())
            .collectList()
            .flatMap(existingTickets -> {
                if (existingTickets.isEmpty()) {
                    return Mono.error(new IllegalStateException(
                        "Order " + order.orderId() + " has no associated tickets"));
                }
                
                boolean allSold = existingTickets.stream()
                    .allMatch(t -> t.status() == TicketStatus.SOLD);
                
                if (allSold) {
                    return Mono.just(order);
                }
                
                boolean allValid = existingTickets.stream()
                    .allMatch(t -> t.status() == TicketStatus.PENDING_CONFIRMATION || 
                                   t.status() == TicketStatus.SOLD);
                
                if (!allValid) {
                    String currentStates = existingTickets.stream()
                        .map(t -> t.status().toString())
                        .distinct()
                        .collect(Collectors.joining(", "));
                    return Mono.error(new InvalidStateTransitionException(
                        "Order",
                        order.orderId(),
                        currentStates,
                        "CONFIRM (requires PENDING_CONFIRMATION state)"
                    ));
                }
                
                List<Ticket> confirmedTickets = existingTickets.stream()
                    .filter(t -> t.status() == TicketStatus.PENDING_CONFIRMATION)
                    .map(Ticket::confirmTicketSale)
                    .toList();
                
                return ticketRepository.saveAll(confirmedTickets)
                    .collectList()
                    .thenReturn(order);
            });
    }
    
    private Mono<Order> updateEventInventory(Order order) {
        return eventRepository.findById(order.eventId())
            .flatMap(event -> {
                return eventRepository.save(event.confirmSale(order.totalTickets()));
            })
            .thenReturn(order)
            .retry(3);
    }
    
    private Mono<Order> confirmOrder(Order order) {
        return orderRepository.save(order.confirm());
    }
}
