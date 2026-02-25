package com.nequi.tickets.domain.repository;

import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.model.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface OrderRepository {
    
    Mono<Order> save(Order order);
    
    Mono<Order> findById(String orderId);
    
    Flux<Order> findByIds(List<String> orderIds);
    
    Flux<Order> findByCustomerId(String customerId);
    
    Flux<Order> findByEventId(String eventId);
    
    Flux<Order> findByStatus(OrderStatus status);
    
    Flux<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);
    
    Flux<Order> findByEventIdAndStatus(String eventId, OrderStatus status);
    
    Flux<Order> findPendingOrders();
    
    Mono<Boolean> existsById(String orderId);
    
    Mono<Long> countByStatus(OrderStatus status);
    
    Mono<Long> countByEventId(String eventId);
    
    Mono<Void> deleteById(String orderId);
}
