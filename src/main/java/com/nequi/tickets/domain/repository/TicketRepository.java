package com.nequi.tickets.domain.repository;

import com.nequi.tickets.domain.model.Ticket;
import com.nequi.tickets.domain.model.TicketStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository {
    
    Mono<Ticket> save(Ticket ticket);
    
    Flux<Ticket> saveAll(List<Ticket> tickets);
    
    Mono<Ticket> findById(String ticketId);
    
    Flux<Ticket> findByEventId(String eventId);
    
    Flux<Ticket> findByIds(List<String> ticketIds);
    
    Flux<Ticket> findByEventIdAndStatus(String eventId, TicketStatus status);
    
    Flux<Ticket> findByCustomerId(String customerId);
    
    Flux<Ticket> findByOrderId(String orderId);
    
    Flux<Ticket> findExpiredReservations(LocalDateTime expirationTime);
    
    Mono<Long> countAvailableByEventId(String eventId);
    
    Mono<Long> countByEventIdAndStatus(String eventId, TicketStatus status);
    
    Mono<Void> deleteById(String ticketId);
    
    Mono<Void> deleteByEventId(String eventId);
}
