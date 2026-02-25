package com.nequi.tickets.usecase;

import com.nequi.tickets.config.BusinessProperties;
import com.nequi.tickets.domain.exception.InsufficientTicketsException;
import com.nequi.tickets.domain.exception.OrderNotFoundException;
import com.nequi.tickets.domain.model.*;
import com.nequi.tickets.domain.repository.EventRepository;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.domain.repository.TicketRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ProcessOrderUseCase {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final BusinessProperties businessProperties;
    
    public ProcessOrderUseCase(
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            BusinessProperties businessProperties) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.businessProperties = businessProperties;
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
                
                if (order.status() == OrderStatus.FAILED || order.status() == OrderStatus.CANCELLED) {
                    return Mono.just(order);
                }
                
                int maxRetries = businessProperties.getOrder().getMaxRetries();
                if (order.retryCount() >= maxRetries) {
                    return failOrder(order, "Maximum retry attempts exceeded");
                }
                
                Order processingOrder = order.startProcessing();
                
                return orderRepository.save(processingOrder)
                    .flatMap(this::processTickets)
                    .onErrorResume(error -> handleProcessingError(processingOrder, error));
            });
    }
    
    private Mono<Order> processTickets(Order order) {
        return ticketRepository.findByOrderId(order.orderId())
            .collectList()
            .flatMap(existingTickets -> {
                if (existingTickets.isEmpty()) {
                    return Mono.error(new IllegalStateException(
                        "Order " + order.orderId() + " has no associated tickets"));
                }
                
                if (existingTickets.size() != order.totalTickets()) {
                    return Mono.error(new InsufficientTicketsException(
                        order.eventId(), order.totalTickets(), existingTickets.size()));
                }
                
                boolean allProcessed = existingTickets.stream()
                    .allMatch(t -> t.status() == TicketStatus.PENDING_CONFIRMATION || 
                                   t.status() == TicketStatus.SOLD);
                
                if (allProcessed) {
                    return Mono.just(order);
                }
                
                List<Ticket> updatedTickets = existingTickets.stream()
                    .filter(t -> t.status() == TicketStatus.RESERVED)
                    .map(Ticket::startConfirmation)
                    .toList();
                
                return ticketRepository.saveAll(updatedTickets)
                    .collectList()
                    .thenReturn(order);
            });
    }
    
    private Mono<Order> handleProcessingError(Order order, Throwable error) {
        
        int maxRetries = businessProperties.getOrder().getMaxRetries();
        if (order.retryCount() >= maxRetries) {
            return releaseTicketsOnFailure(order)
                .then(failOrder(order, "Processing failed after max retries: " + error.getMessage()));
        }
        
        return Mono.error(error);
    }
    
    private Mono<Void> releaseTicketsOnFailure(Order order) {
        return ticketRepository.findByOrderId(order.orderId())
            .collectList()
            .flatMap(tickets -> {
                if (tickets.isEmpty()) {
                    return Mono.empty();
                }
                
                List<Ticket> releasedTickets = tickets.stream()
                    .filter(t -> t.status() == TicketStatus.RESERVED || 
                                 t.status() == TicketStatus.PENDING_CONFIRMATION)
                    .map(Ticket::releaseTicketBack)
                    .toList();
                
                return ticketRepository.saveAll(releasedTickets)
                    .collectList()
                    .flatMap(saved -> eventRepository.findById(order.eventId())
                        .flatMap(event -> {
                            Event releasedEvent = event.releaseReservedTickets(order.totalTickets());
                            return eventRepository.save(releasedEvent);
                        })
                        .retry(3)
                    )
                    .then();
            })
            .onErrorResume(error -> {
                System.err.println("Failed to release tickets for order " + order.orderId() + ": " + error.getMessage());
                return Mono.<Void>empty();
            });
    }
    
    private Mono<Order> failOrder(Order order, String reason) {
        return orderRepository.save(order.fail(reason));
    }
}
